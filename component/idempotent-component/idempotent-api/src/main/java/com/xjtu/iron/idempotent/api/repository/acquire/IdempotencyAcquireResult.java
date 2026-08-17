package com.xjtu.iron.idempotent.api.repository.acquire;

import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;

/** 普通 tryAcquire 的决策结果。 */
public final class IdempotencyAcquireResult {

    private final IdempotencyAcquireStatus status;
    private final IdempotencyRecord record;
    private final boolean windowRollover;
    private final Throwable error;

    private IdempotencyAcquireResult(
            IdempotencyAcquireStatus status,
            IdempotencyRecord record,
            boolean windowRollover,
            Throwable error) {
        this.status = status;
        this.record = record;
        this.windowRollover = windowRollover;
        this.error = error;
    }

    public static IdempotencyAcquireResult of(
            IdempotencyAcquireStatus status,
            IdempotencyRecord record) {
        return new IdempotencyAcquireResult(status, record, false, null);
    }

    public static IdempotencyAcquireResult acquired(
            IdempotencyRecord record,
            boolean windowRollover) {
        return new IdempotencyAcquireResult(
                IdempotencyAcquireStatus.ACQUIRED,
                record,
                windowRollover,
                null);
    }

    public static IdempotencyAcquireResult providerError(Throwable error) {
        return new IdempotencyAcquireResult(
                IdempotencyAcquireStatus.PROVIDER_ERROR,
                null,
                false,
                error);
    }

    public IdempotencyAcquireStatus getStatus() { return status; }
    public IdempotencyRecord getRecord() { return record; }

    /** WINDOWED 语义窗口结束后，是否由旧物理记录滚动开启了新 generation。 */
    public boolean isWindowRollover() { return windowRollover; }

    /** @deprecated V1.1 起请使用 {@link #isWindowRollover()}。 */
    @Deprecated
    public boolean isTakeover() { return windowRollover; }

    public Throwable getError() { return error; }
}
