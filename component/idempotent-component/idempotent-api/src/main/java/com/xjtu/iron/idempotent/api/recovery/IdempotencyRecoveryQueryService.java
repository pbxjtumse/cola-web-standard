package com.xjtu.iron.idempotent.api.recovery;

import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryCandidate;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryQuery;

import java.util.List;

/**
 * 提供给外部 Reliable Task 的恢复候选查询入口。
 *
 * <p>该接口只查询候选，不创建调度线程、不发 MQ、不做任务分片。
 * 扫描器统一通过 policyName 复用正常 execute/recover 的 Policy，
 * 避免恢复配置与运行时配置发生漂移。</p>
 */
public interface IdempotencyRecoveryQueryService {

    List<IdempotencyRecoveryCandidate> findCandidates(String policyName, IdempotencyRecoveryQuery query);
}
