package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.IdempotencyMode;

import java.util.Optional;

/**
 * 幂等状态正确性的核心 SPI。
 *
 * <p>这是整个组件最重要的扩展点。DistributedLockClient 只能减少竞争，
 * 即使完全关闭分布式锁，Repository 自己也必须保证以下操作的原子状态语义。</p>
 *
 * <p>不同实现的典型保证方式：</p>
 * <ul>
 *     <li>JDBC：UNIQUE KEY、短事务、SELECT FOR UPDATE、owner/version 条件更新；</li>
 *     <li>Redis：Lua 一次完成读取、判断和修改。</li>
 * </ul>
 */
public interface IdempotencyRepository {

    /**
     * Provider 唯一名称，例如 jdbc / redis。
     * 用于 Registry 选择、配置和指标标签。
     */
    String providerName();

    /**
     * 当前 Repository 是否支持指定模式。
     * 例如 Redis Provider V1.1 默认只支持 SHORT_TERM。
     */
    boolean supports(IdempotencyMode mode);

    /**
     * 普通 execute() 的原子状态判定/抢占入口。
     *
     * <p>必须满足：</p>
     * <ul>
     *     <li>记录不存在时，最多只有一个调用者能创建 PROCESSING；</li>
     *     <li>已有 SUCCESS 时返回 SUCCESS，不再执行业务；</li>
     *     <li>PROCESSING 超时时只返回 PROCESSING_EXPIRED，不自动恢复；</li>
     *     <li>SHORT_TERM 窗口到期时可原子开启新 generation。</li>
     * </ul>
     */
    IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request);

    /**
     * 显式 recover() 的原子接管入口。
     *
     * <p>只有 Reliable Task 等恢复调用方应使用。实现必须再次校验 expectedOwner/version，
     * 避免扫描候选在排队期间已经失效却仍被执行。</p>
     */
    IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request);

    /**
     * 当前 generation 完成成功。
     *
     * <p>必须使用 ownerToken + version 条件写，旧 owner 不能覆盖新 generation。</p>
     */
    IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request);

    /**
     * 当前 generation 完成失败。
     *
     * <p>同样必须使用 ownerToken + version 条件写。</p>
     */
    IdempotencyWriteResult markFailed(IdempotencyFailureRequest request);

    /**
     * 读取当前状态快照。
     *
     * <p>该查询用于诊断、展示或辅助流程，不应通过“find -> Java 判断 -> update”
     * 自行实现并发状态转换。</p>
     */
    Optional<IdempotencyRecord> find(String namespace, String key);
}
