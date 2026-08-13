package com.xjtu.iron.idempotent.api;

import com.xjtu.iron.idempotent.api.repository.IdempotencyRecoveryCandidate;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRecoveryQuery;

import java.util.List;

/**
 * 提供给外部 Reliable Task 的“恢复候选查询”入口。
 *
 * <p>该接口只查询，不创建调度线程、不发 MQ、不做任务分片。</p>
 */
public interface IdempotencyRecoveryQueryService {

    List<IdempotencyRecoveryCandidate> findCandidates(
            IdempotencyMode mode,
            String repositoryName,
            IdempotencyRecoveryQuery query);
}
