package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.IdempotencyMode;

import java.util.Optional;

/**
 * 幂等状态存储 SPI。
 *
 * <p>这是整个组件最重要的正确性边界。实现必须满足以下契约：</p>
 * <ol>
 *     <li>{@link #tryAcquire(IdempotencyAcquireRequest)} 的“读取 + 判断 + 状态转换”必须原子；</li>
 *     <li>同一 namespace + key 同时只能有一个 owner 获得当前 version 的 PROCESSING 执行权；</li>
 *     <li>{@code markSuccess/markFailed} 必须校验 ownerToken + version；</li>
 *     <li>旧 owner 恢复后不能覆盖新 owner 已经产生的状态；</li>
 *     <li>PROCESSING 超时恢复必须通过 CAS / 行锁 / Lua 等原子机制完成。</li>
 * </ol>
 *
 * <p>分布式锁不是该 SPI 正确性的前提。即使完全没有 DistributedLockClient，Repository
 * 也必须独立满足上述契约。</p>
 */
public interface IdempotencyRepository {

    /** Provider 唯一名称，例如 redis / jdbc。 */
    String providerName();

    /** 当前 Repository 是否支持指定持久化模式。 */
    boolean supports(IdempotencyMode mode);

    /**
     * 尝试获取 key 的业务执行权。
     *
     * <p>可能返回 ACQUIRED、SUCCESS、PROCESSING、FAILED、KEY_CONFLICT 或 PROVIDER_ERROR。</p>
     */
    IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request);

    /**
     * 将当前 owner 的 PROCESSING 标记为 SUCCESS。
     * 必须同时比较 ownerToken 和 version。
     */
    IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request);

    /**
     * 将当前 owner 的 PROCESSING 标记为 FAILED。
     * 必须同时比较 ownerToken 和 version。
     */
    IdempotencyWriteResult markFailed(IdempotencyFailureRequest request);

    /** 查询当前状态快照。 */
    Optional<IdempotencyRecord> find(String namespace, String key);
}
