package com.xjtu.iron.idempotent.api;

import com.xjtu.iron.idempotent.api.repository.IdempotencyRecord;

import java.util.Objects;
import java.util.Optional;

/**
 * 一次 {@link IdempotencyExecutor} 调用的统一结果。
 *
 * <p>设计上与 distributed-lock 的 {@code LockResult} 保持相似风格：</p>
 * <ul>
 *     <li>{@code status} 表示调用最终结果；</li>
 *     <li>{@code stage} 表示结果产生在哪个阶段；</li>
 *     <li>{@code record} 保存当时的幂等状态快照；</li>
 *     <li>{@code lockFallback} 表示可选分布式锁失败后是否退化为 Repository 原子抢占。</li>
 * </ul>
 *
 * @param <T> 业务结果类型
 */
public final class IdempotencyResult<T> {

    private final IdempotencyResultStatus status;
    private final IdempotencyStage stage;
    private final T value;
    private final IdempotencyRecord record;
    private final Throwable error;
    private final boolean lockFallback;

    private IdempotencyResult(Builder<T> builder) {
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.stage = Objects.requireNonNull(builder.stage, "stage must not be null");
        this.value = builder.value;
        this.record = builder.record;
        this.error = builder.error;
        this.lockFallback = builder.lockFallback;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * EXECUTED / RECOVERED / REPLAYED 都属于调用方可直接使用的成功结果。
     */
    public boolean isSuccess() {
        return status == IdempotencyResultStatus.EXECUTED
                || status == IdempotencyResultStatus.RECOVERED
                || status == IdempotencyResultStatus.REPLAYED;
    }

    public IdempotencyResultStatus status() {
        return status;
    }

    public IdempotencyResultStatus getStatus() {
        return status;
    }

    public IdempotencyStage stage() {
        return stage;
    }

    public IdempotencyStage getStage() {
        return stage;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public T getValue() {
        return value;
    }

    public Optional<IdempotencyRecord> record() {
        return Optional.ofNullable(record);
    }

    public IdempotencyRecord getRecord() {
        return record;
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public Throwable getError() {
        return error;
    }

    public boolean lockFallback() {
        return lockFallback;
    }

    public boolean isLockFallback() {
        return lockFallback;
    }

    public static final class Builder<T> {
        private IdempotencyResultStatus status;
        private IdempotencyStage stage;
        private T value;
        private IdempotencyRecord record;
        private Throwable error;
        private boolean lockFallback;

        public Builder<T> status(IdempotencyResultStatus value) {
            this.status = value;
            return this;
        }

        public Builder<T> stage(IdempotencyStage value) {
            this.stage = value;
            return this;
        }

        public Builder<T> value(T value) {
            this.value = value;
            return this;
        }

        public Builder<T> record(IdempotencyRecord value) {
            this.record = value;
            return this;
        }

        public Builder<T> error(Throwable value) {
            this.error = value;
            return this;
        }

        public Builder<T> lockFallback(boolean value) {
            this.lockFallback = value;
            return this;
        }

        public IdempotencyResult<T> build() {
            return new IdempotencyResult<>(this);
        }
    }
}
