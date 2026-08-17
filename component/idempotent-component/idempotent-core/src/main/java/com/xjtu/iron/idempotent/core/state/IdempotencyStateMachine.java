package com.xjtu.iron.idempotent.core.state;

import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryStatus;

/**
 * 幂等状态机的“决策层”。
 *
 * <p>Repository 负责原子状态转换；本接口只把 Provider 返回的判定状态
 * 翻译成 EXECUTE / REPLAY / RETURN 三类稳定动作。</p>
 */
public interface IdempotencyStateMachine {

    IdempotencyStateDecision onAcquire(IdempotencyAcquireStatus status);

    IdempotencyStateDecision onRecovery(IdempotencyRecoveryStatus status);
}
