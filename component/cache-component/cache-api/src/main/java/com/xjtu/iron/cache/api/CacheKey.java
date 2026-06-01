package com.xjtu.iron.cache.api;

import java.util.Objects;

/**
 * 缓存 key 模型。
 *
 * <p>不要在业务代码中直接拼接 Redis key，而应该通过 CacheKey 统一构建。
 * 这样可以统一 key 规范、方便排查、迁移和版本升级。</p>
 *
 * <p>一期推荐 key 结构：</p>
 *
 * <pre>
 * namespace:cacheName:bizKey:version
 * </pre>
 *
 * <p>例如：</p>
 *
 * <pre>
 * marketing:campaignRule:campaignId:10001:v1
 * </pre>
 *
 * <p>如果 Redis 配置了 key-prefix，例如 cache:，最终 Redis key 会变成：</p>
 *
 * <pre>
 * cache:marketing:campaignRule:campaignId:10001:v1
 * </pre>
 */
public final class CacheKey {

    /**
     * 缓存名称。
     *
     * <p>cacheName 用于绑定缓存策略，例如 campaignRule、userProfile、merchantConfig。</p>
     */
    private final String cacheName;

    /**
     * 命名空间。
     *
     * <p>通常用业务域、应用名或领域名，例如 marketing、merchant、trade。</p>
     */
    private final String namespace;

    /**
     * 业务 key。
     *
     * <p>用于表达具体业务对象，例如 campaignId:10001、userId:9527。</p>
     */
    private final String bizKey;

    /**
     * key 版本。
     *
     * <p>当缓存 value 结构变化时，可以通过升级 version 直接切换到新 key，避免旧数据反序列化失败。</p>
     */
    private final String version;

    /**
     * 私有构造方法，强制外部通过静态工厂方法创建 CacheKey。
     */
    private CacheKey(String cacheName, String namespace, String bizKey, String version) {
        this.cacheName = requireText(cacheName, "cacheName");
        this.namespace = requireText(namespace, "namespace");
        this.bizKey = requireText(bizKey, "bizKey");
        this.version = requireText(version, "version");
    }

    /**
     * 创建默认 v1 版本的缓存 key。
     */
    public static CacheKey of(String cacheName, String namespace, String bizKey) {
        return new CacheKey(cacheName, namespace, bizKey, "v1");
    }

    /**
     * 创建指定版本的缓存 key。
     */
    public static CacheKey of(String cacheName, String namespace, String bizKey, String version) {
        return new CacheKey(cacheName, namespace, bizKey, version);
    }

    /**
     * 返回缓存名称。
     */
    public String cacheName() {
        return cacheName;
    }

    /**
     * 返回命名空间。
     */
    public String namespace() {
        return namespace;
    }

    /**
     * 返回业务 key。
     */
    public String bizKey() {
        return bizKey;
    }

    /**
     * 返回 key 版本。
     */
    public String version() {
        return version;
    }

    /**
     * 返回不带 Redis 全局前缀的完整逻辑 key。
     */
    public String fullKey() {
        return namespace + ":" + cacheName + ":" + bizKey + ":" + version;
    }

    /**
     * 校验文本字段不能为空。
     */
    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }

    /**
     * 打印时直接展示完整逻辑 key，方便日志排查。
     */
    @Override
    public String toString() {
        return fullKey();
    }
}
