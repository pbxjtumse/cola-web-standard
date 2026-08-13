package com.xjtu.iron.idempotent.api.repository;

import java.util.List;

/**
 * 可被 Reliable Task 查询的 Repository 扩展能力。
 *
 * <p>目前 JDBC Provider 实现；SHORT_TERM Redis 默认不实现扫描能力。</p>
 */
public interface IdempotencyRecoveryRepository {
    List<IdempotencyRecoveryCandidate> findRecoveryCandidates(IdempotencyRecoveryQuery query);
}
