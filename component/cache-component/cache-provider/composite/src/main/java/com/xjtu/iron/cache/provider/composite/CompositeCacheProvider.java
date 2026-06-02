package com.xjtu.iron.cache.provider.composite;

import com.xjtu.iron.cache.api.key.CacheKey;
import com.xjtu.iron.cache.api.model.CacheSpec;
import com.xjtu.iron.cache.api.model.CacheValue;
import com.xjtu.iron.cache.core.CacheProvider;

import java.time.Duration;

/**
 * 二级缓存组合 Provider。
 *
 * <p>这个类是一期多级缓存的核心组合层，但它本身不直接依赖 Caffeine 或 Redis 的具体类。</p>
 *
 * <p>一期默认约定：</p>
 *
 * <pre>
 * L1 = 本地缓存，例如 CaffeineCacheProvider
 * L2 = 分布式缓存，例如 RedisCacheProvider
 * </pre>
 *
 * <p>为什么字段命名为 l1Provider / l2Provider，而不是 caffeineProvider / redisProvider？</p>
 *
 * <pre>
 * 1. CompositeCacheProvider 只关心缓存层级，不关心具体技术实现；
 * 2. 后续如果 L1 从 Caffeine 换成其他本地缓存，Composite 层不用改；
 * 3. 后续如果 L2 从 Redis 换成其他分布式 KV，Composite 层也不用改。
 * </pre>
 *
 * <p>读取流程：</p>
 *
 * <pre>
 * 1. 先查 L1；
 * 2. L1 miss 后查 L2；
 * 3. L2 hit 后回填 L1；
 * 4. L1、L2 都 miss，返回 CacheValue.miss()。
 * </pre>
 *
 * <p>一期只负责当前实例内的二级缓存读写。多实例 L1 失效通知属于二期能力，不混入一期。</p>
 */
public class CompositeCacheProvider implements CacheProvider {

    /**
     * 一级缓存 Provider。
     *
     * <p>一期实际注入 CaffeineCacheProvider，但这里按抽象 CacheProvider 持有。</p>
     */
    private final CacheProvider l1Provider;

    /**
     * 二级缓存 Provider。
     *
     * <p>一期实际注入 RedisCacheProvider，但这里按抽象 CacheProvider 持有。</p>
     */
    private final CacheProvider l2Provider;

    /**
     * 创建二级缓存组合 Provider。
     *
     * @param l1Provider 一级缓存 Provider，通常是 Caffeine
     * @param l2Provider 二级缓存 Provider，通常是 Redis
     */
    public CompositeCacheProvider(CacheProvider l1Provider, CacheProvider l2Provider) {
        this.l1Provider = l1Provider;
        this.l2Provider = l2Provider;
    }

    /**
     * 返回 Provider 名称。
     *
     * @return provider 名称
     */
    @Override
    public String name() {
        return "composite";
    }

    /**
     * 按 L1 -> L2 的顺序读取缓存。
     *
     * @param key 缓存 key
     * @param valueType 值类型
     * @param spec 缓存策略
     * @return 缓存值结果
     */
    @Override
    public <T> CacheValue<T> get(CacheKey key, Class<T> valueType, CacheSpec spec) {
        if (spec.isEnableL1()) {
            CacheValue<T> l1Value = l1Provider.get(key, valueType, spec);
            if (l1Value.isPresent()) {
                return l1Value;
            }
        }

        if (spec.isEnableL2()) {
            CacheValue<T> l2Value = l2Provider.get(key, valueType, spec);
            if (l2Value.isPresent()) {
                backfillL1(key, l2Value, spec);
                return l2Value;
            }
        }

        return CacheValue.miss();
    }

    /**
     * L2 命中后回填 L1。
     *
     * <p>这样下一次同一个 JVM 实例访问该 key 时，可以直接命中本地缓存，减少 Redis 压力。</p>
     *
     * <p>当前回填使用 CacheSpec 的配置 TTL。更精细的做法是将已计算 TTL 透传到这里，
     * 但一期为保持结构简单，先不引入更多上下文对象。</p>
     *
     * @param key 缓存 key
     * @param value L2 命中的缓存值
     * @param spec 缓存策略
     */
    private <T> void backfillL1(CacheKey key, CacheValue<T> value, CacheSpec spec) {
        if (!spec.isEnableL1()) {
            return;
        }

        if (value.isNullValue()) {
            l1Provider.putNullValue(key, spec.getNullValueTtl(), spec);
            return;
        }

        l1Provider.put(key, value.getValue(), spec.getTtl(), spec);
    }

    /**
     * 写入正常值。
     *
     * <p>一期默认先写 L2，再写 L1。这样 Redis 作为跨实例共享缓存，优先完成写入。</p>
     *
     * @param key 缓存 key
     * @param value 正常缓存值
     * @param ttl 本次写入 TTL
     * @param spec 缓存策略
     */
    @Override
    public void put(CacheKey key, Object value, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.put(key, value, ttl, spec);
        }

        if (spec.isEnableL1()) {
            l1Provider.put(key, value, ttl, spec);
        }
    }

    /**
     * 写入空值占位。
     *
     * <p>空值占位用于防止缓存穿透。空值 TTL 通常明显短于正常值 TTL。</p>
     *
     * @param key 缓存 key
     * @param ttl 空值 TTL
     * @param spec 缓存策略
     */
    @Override
    public void putNullValue(CacheKey key, Duration ttl, CacheSpec spec) {
        if (spec.isEnableL2()) {
            l2Provider.putNullValue(key, ttl, spec);
        }

        if (spec.isEnableL1()) {
            l1Provider.putNullValue(key, ttl, spec);
        }
    }

    /**
     * 删除缓存。
     *
     * <p>一期只删除当前实例 L1 和 Redis L2。多实例本地缓存广播失效放到二期。</p>
     *
     * @param key 缓存 key
     */
    @Override
    public void evict(CacheKey key) {
        l1Provider.evict(key);
        l2Provider.evict(key);
    }
}
