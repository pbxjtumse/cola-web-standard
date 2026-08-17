package com.xjtu.iron.idempotent.api.repository;

import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireResult;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryAcquireRequest;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryResult;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureRequest;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencySuccessRequest;
import com.xjtu.iron.idempotent.api.repository.write.IdempotencyWriteResult;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;

import java.util.Optional;

/**
 * 幂等状态正确性的核心 SPI。
 *
 * <p>DistributedLockClient 只能减少竞争；即使完全关闭分布式锁，
 * Repository 自己也必须依靠 UNIQUE / 行锁 / Lua / CAS 保证原子状态转换。</p>
 */
public interface IdempotencyRepository {

    String providerName();

    /**
     * Provider 能力声明。Core 不再通过 providerName 猜测语义。
     */
    IdempotencyRepositoryCapabilities capabilities();

    default boolean supports(IdempotencyMode mode) {
        return capabilities().supports(mode);
    }

    default boolean supportsBusinessTransactionParticipation() {
        return capabilities().isBusinessTransactionParticipationSupported();
    }

    IdempotencyAcquireResult tryAcquire(IdempotencyAcquireRequest request);

    IdempotencyRecoveryResult tryRecover(IdempotencyRecoveryAcquireRequest request);

    IdempotencyWriteResult markSuccess(IdempotencySuccessRequest request);

    IdempotencyWriteResult markFailed(IdempotencyFailureRequest request);

    Optional<IdempotencyRecord> find(String namespace, String key);
}
