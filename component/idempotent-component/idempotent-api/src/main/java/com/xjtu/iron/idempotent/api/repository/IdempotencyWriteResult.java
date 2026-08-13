package com.xjtu.iron.idempotent.api.repository;

/**
 * Repository 完成状态写入后的统一返回值。
 *
 * <p>这里不直接返回 boolean，是因为调用方必须区分：</p>
 * <ul>
 *     <li>{@link IdempotencyWriteStatus#UPDATED}：当前 owner/version 成功完成状态转换；</li>
 *     <li>{@link IdempotencyWriteStatus#STALE_OWNER}：执行权已经被新的 generation 接管；</li>
 *     <li>{@link IdempotencyWriteStatus#ALREADY_FINAL}：记录已经进入终态，旧执行者不能覆盖；</li>
 *     <li>{@link IdempotencyWriteStatus#PROVIDER_ERROR}：存储层异常。</li>
 * </ul>
 */
public final class IdempotencyWriteResult {

    /** 本次写操作的语义状态。 */
    private final IdempotencyWriteStatus status;

    /** 写入后或冲突时观察到的最新状态快照。 */
    private final IdempotencyRecord record;

    /** Provider 异常；只有 PROVIDER_ERROR 时通常非空。 */
    private final Throwable error;

    private IdempotencyWriteResult(
            IdempotencyWriteStatus status,
            IdempotencyRecord record,
            Throwable error) {
        this.status = status;
        this.record = record;
        this.error = error;
    }

    public static IdempotencyWriteResult of(
            IdempotencyWriteStatus status,
            IdempotencyRecord record) {
        return new IdempotencyWriteResult(status, record, null);
    }

    public static IdempotencyWriteResult providerError(Throwable error) {
        return new IdempotencyWriteResult(
                IdempotencyWriteStatus.PROVIDER_ERROR, null, error);
    }

    public IdempotencyWriteStatus getStatus() {
        return status;
    }

    public IdempotencyRecord getRecord() {
        return record;
    }

    public Throwable getError() {
        return error;
    }
}
