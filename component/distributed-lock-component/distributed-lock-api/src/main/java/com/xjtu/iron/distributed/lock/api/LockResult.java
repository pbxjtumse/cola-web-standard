package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * 分布式锁操作结果。
 *
 * <p>该对象同时用于 tryLock 和 execute 两类场景。需要特别区分两个概念：</p>
 * <ul>
 *     <li>{@code status}：本次操作的最终状态，例如 SUCCESS、NOT_ACQUIRED、EXECUTION_FAILED。</li>
 *     <li>{@code acquired}：本次操作是否曾经成功获取过锁。</li>
 * </ul>
 *
 * <p>例如 execute 场景下，业务执行失败时 status 是 EXECUTION_FAILED，但 acquired 仍然应该是 true，
 * 因为模板确实拿到过锁。</p>
 *
 * @param <T> 业务返回值类型。
 */
public final class LockResult<T> {

    /** 最终状态。 */
    private final LockStatus status;

    /** 是否曾经成功获取锁。 */
    private final boolean acquired;

    /** 业务返回值。 */
    private final T value;

    /** 加锁成功后生成的锁句柄。 */
    private final LockHandle handle;

    /** 异常信息。 */
    private final Throwable error;

    /** 业务锁名称。 */
    private final String lockName;

    /** Provider 真实锁 key 或路径。 */
    private final String lockKey;

    /** 本次锁租约 ownerToken。 */
    private final String ownerToken;

    /** 本次锁租约 fencingToken。 */
    private final Long fencingToken;

    /** 获取锁等待耗时。 */
    private final Duration waitDuration;

    /** 持锁耗时。 */
    private final Duration holdDuration;

    /** 附加消息。 */
    private final String message;

    private LockResult(Builder<T> builder) {
        this.status = builder.status;
        this.acquired = builder.acquired;
        this.value = builder.value;
        this.handle = builder.handle;
        this.error = builder.error;
        this.lockName = builder.lockName;
        this.lockKey = builder.lockKey;
        this.ownerToken = builder.ownerToken;
        this.fencingToken = builder.fencingToken;
        this.waitDuration = builder.waitDuration;
        this.holdDuration = builder.holdDuration;
        this.message = builder.message;
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static LockResult<LockHandle> acquired(LockHandle handle, Duration waitDuration) {
        return LockResult.<LockHandle>builder()
                .status(LockStatus.ACQUIRED)
                .acquired(true)
                .value(handle)
                .handle(handle)
                .lockName(handle == null ? null : handle.lockName())
                .lockKey(handle == null ? null : handle.lockKey())
                .ownerToken(handle == null ? null : handle.ownerToken())
                .fencingToken(handle == null || handle.fencingToken().isEmpty() ? null : handle.fencingToken().getAsLong())
                .waitDuration(waitDuration)
                .build();
    }

    public static <T> LockResult<T> success(T value, LockHandle handle, Duration waitDuration, Duration holdDuration) {
        return LockResult.<T>builder()
                .status(LockStatus.SUCCESS)
                .acquired(true)
                .value(value)
                .handle(handle)
                .lockName(handle == null ? null : handle.lockName())
                .lockKey(handle == null ? null : handle.lockKey())
                .ownerToken(handle == null ? null : handle.ownerToken())
                .fencingToken(handle == null || handle.fencingToken().isEmpty() ? null : handle.fencingToken().getAsLong())
                .waitDuration(waitDuration)
                .holdDuration(holdDuration)
                .build();
    }

    public static <T> LockResult<T> notAcquired(String lockName, Duration waitDuration) {
        return LockResult.<T>builder()
                .status(LockStatus.NOT_ACQUIRED)
                .acquired(false)
                .lockName(lockName)
                .waitDuration(waitDuration)
                .build();
    }

    public static <T> LockResult<T> failed(LockStatus status, String lockName, boolean acquired, Throwable error) {
        return LockResult.<T>builder()
                .status(status)
                .acquired(acquired)
                .lockName(lockName)
                .error(error)
                .message(error == null ? null : error.getMessage())
                .build();
    }

    public LockStatus getStatus() {
        return status;
    }

    public boolean isAcquired() {
        return acquired;
    }

    public boolean isSuccess() {
        return status == LockStatus.SUCCESS || status == LockStatus.ACQUIRED;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<LockHandle> handle() {
        return Optional.ofNullable(handle);
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockKey() {
        return lockKey;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public OptionalLong fencingToken() {
        return fencingToken == null ? OptionalLong.empty() : OptionalLong.of(fencingToken);
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }

    public Duration getHoldDuration() {
        return holdDuration;
    }

    public String getMessage() {
        return message;
    }

    /**
     * LockResult 构造器。
     *
     * @param <T> 业务返回值类型。
     */
    public static final class Builder<T> {

        private LockStatus status;
        private boolean acquired;
        private T value;
        private LockHandle handle;
        private Throwable error;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private Long fencingToken;
        private Duration waitDuration;
        private Duration holdDuration;
        private String message;

        private Builder() {
        }

        public Builder<T> status(LockStatus status) {
            this.status = status;
            return this;
        }

        public Builder<T> acquired(boolean acquired) {
            this.acquired = acquired;
            return this;
        }

        public Builder<T> value(T value) {
            this.value = value;
            return this;
        }

        public Builder<T> handle(LockHandle handle) {
            this.handle = handle;
            return this;
        }

        public Builder<T> error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder<T> lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder<T> lockKey(String lockKey) {
            this.lockKey = lockKey;
            return this;
        }

        public Builder<T> ownerToken(String ownerToken) {
            this.ownerToken = ownerToken;
            return this;
        }

        public Builder<T> fencingToken(Long fencingToken) {
            this.fencingToken = fencingToken;
            return this;
        }

        public Builder<T> waitDuration(Duration waitDuration) {
            this.waitDuration = waitDuration;
            return this;
        }

        public Builder<T> holdDuration(Duration holdDuration) {
            this.holdDuration = holdDuration;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public LockResult<T> build() {
            return new LockResult<>(this);
        }
    }
}
