package com.xjtu.iron.idempotent.core.state;

import com.xjtu.iron.idempotent.api.execution.IdempotencyResultStatus;
import com.xjtu.iron.idempotent.api.repository.acquire.IdempotencyAcquireStatus;
import com.xjtu.iron.idempotent.api.repository.recovery.IdempotencyRecoveryStatus;

/**
 * 默认幂等状态机。
 */
public final class DefaultIdempotencyStateMachine implements IdempotencyStateMachine {

    @Override
    public IdempotencyStateDecision onAcquire(IdempotencyAcquireStatus status) {
        return switch (status) {
            case ACQUIRED -> IdempotencyStateDecision.execute();
            case SUCCESS -> IdempotencyStateDecision.replay();
            case PROCESSING_ACTIVE ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.PROCESSING);
            case PROCESSING_EXPIRED ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.PROCESSING_EXPIRED);
            case FAILED_RETRYABLE ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.PREVIOUS_FAILED_RETRYABLE);
            case FAILED_FINAL ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.PREVIOUS_FAILED_FINAL);
            case KEY_CONFLICT ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.KEY_CONFLICT);
            case PROVIDER_ERROR ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.REPOSITORY_ERROR);
        };
    }

    @Override
    public IdempotencyStateDecision onRecovery(IdempotencyRecoveryStatus status) {
        return switch (status) {
            case RECOVERY_ACQUIRED -> IdempotencyStateDecision.execute();
            case SUCCESS -> IdempotencyStateDecision.replay();
            case PROCESSING_ACTIVE ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.PROCESSING);
            case NOT_RECOVERABLE, FAILED_FINAL, NOT_FOUND ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.RECOVERY_NOT_ALLOWED);
            case STALE_CANDIDATE ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.STALE_RECOVERY_CANDIDATE);
            case KEY_CONFLICT ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.KEY_CONFLICT);
            case PROVIDER_ERROR ->
                    IdempotencyStateDecision.returning(IdempotencyResultStatus.REPOSITORY_ERROR);
        };
    }
}
