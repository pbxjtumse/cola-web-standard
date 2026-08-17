package com.xjtu.iron.idempotent.api.recovery;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;

import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryCandidate;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryQuery;

import java.util.List;

/**
 * 提供给外部 Reliable Task 的“恢复候选查询”入口。
 *
 * <p>该接口只查询，不创建调度线程、不发 MQ、不做任务分片。</p>
 *
 * <p>V1.3 优先按 policyName 查询。这样扫描器与正常 execute/recover 使用同一份
 * 生命周期、Repository 和恢复语义，不需要任务组件再次手工拼 mode + repositoryName。</p>
 */
public interface IdempotencyRecoveryQueryService {

    /**
     * 推荐入口：按命名策略查询恢复候选。
     */
    List<IdempotencyRecoveryCandidate> findCandidates(
            String policyName,
            IdempotencyRecoveryQuery query);

    /**
     * V1.2 兼容入口。
     *
     * @deprecated V1.3 起优先使用 policyName，避免恢复扫描与运行时 Policy 漂移。
     */
    @Deprecated
    List<IdempotencyRecoveryCandidate> findCandidates(
            IdempotencyMode mode,
            String repositoryName,
            IdempotencyRecoveryQuery query);
}
