package com.xjtu.iron.distributed.lock.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * 分布式锁操作结果。
 *
 * <p>该对象统一承载 tryLock 和 execute 的返回信息，包括状态、业务返回值、锁句柄、异常、等待耗时、持锁耗时、
 * ownerToken、fencingToken 等。</p>
 *
 * @param <T> 返回值类型。tryLock 时通常为 {@link LockHandle}，execute 时为业务返回值类型。
 */
public final class LockResult<T> {

    /**
     * 结果状态。
     */
    private final LockStatus status;

    /**
     * 业务返回值。
     *
     * <p>仅 execute 成功时通常存在。tryLock 场景下不建议使用该字段，应使用 handle。</p>
     */
    private final T value;

    /**
     * 成功获取锁后的锁句柄。
     *
     * <p>tryLock 成功时存在；execute 成功或业务失败时也可以保留，用于记录 ownerToken/fencingToken 等信息。</p>
     */
    private final LockHandle handle;

    /**
     * 失败异常。
     *
     * <p>业务异常、Provider 异常、锁丢失异常、fencing 拒绝异常均可以记录在这里。</p>
     */
    private final Throwable error;

    /**
     * 业务锁名称。
     */
    private final String lockName;

    /**
     * 底层真实锁 key。
     */
    private final String lockKey;

    /**
     * 本次锁租约 ownerToken。
     */
    private final String ownerToken;

    /**
     * 本次锁租约 fencingToken。
     */
    private final Long fencingToken;

    /**
     * 等待获取锁耗时。
     */
    private final Duration waitDuration;

    /**
     * 持锁业务执行耗时。
     */
    private final Duration holdDuration;

    private LockResult(Builder<T> builder) {
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.value = builder.value;
        this.handle = builder.handle;
        this.error = builder.error;
        this.lockName = builder.lockName;
        this.lockKey = builder.lockKey;
        this.ownerToken = builder.ownerToken;
        this.fencingToken = builder.fencingToken;
        this.waitDuration = builder.waitDuration == null ? Duration.ZERO : builder.waitDuration;
        this.holdDuration = builder.holdDuration == null ? Duration.ZERO : builder.holdDuration;
    }

    /**
     * 创建构造器。
     *
     * @param <T> 返回值类型。
     * @return 构造器。
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * 构造 tryLock 成功结果。
     *
     * @param handle       锁句柄。
     * @param waitDuration 等待耗时。
     * @return 成功结果。
     */
    public static LockResult<LockHandle> acquired(LockHandle handle, Duration waitDuration) {
        Objects.requireNonNull(handle, "handle must not be null");
        return LockResult.<LockHandle>builder()
                .status(LockStatus.ACQUIRED)
                .value(handle)
                .handle(handle)
                .lockName(handle.lockName())
                .lockKey(handle.lockKey())
                .ownerToken(handle.ownerToken())
                .fencingToken(handle.fencingToken().isPresent() ? handle.fencingToken().getAsLong() : null)
                .waitDuration(waitDuration)
                .build();
    }

    /**
     * 构造未获取锁结果。
     *
     * @param lockName     业务锁名称。
     * @param lockKey      底层锁 key。
     * @param waitDuration 等待耗时。
     * @param <T>          返回值类型。
     * @return 未获取锁结果。
     */
    public static <T> LockResult<T> notAcquired(String lockName, String lockKey, Duration waitDuration) {
        return LockResult.<T>builder()
                .status(LockStatus.NOT_ACQUIRED)
                .lockName(lockName)
                .lockKey(lockKey)
                .waitDuration(waitDuration)
                .build();
    }

    /**
     * 构造 execute 成功结果。
     *
     * @param value        业务返回值。
     * @param handle       锁句柄。
     * @param waitDuration 等待耗时。
     * @param holdDuration 持锁耗时。
     * @param <T>          业务返回值类型。
     * @return 成功结果。
     */
    public static <T> LockResult<T> success(T value, LockHandle handle, Duration waitDuration, Duration holdDuration) {
        return LockResult.<T>builder()
                .status(LockStatus.SUCCESS)
                .value(value)
                .handle(handle)
                .lockName(handle == null ? null : handle.lockName())
                .lockKey(handle == null ? null : handle.lockKey())
                .ownerToken(handle == null ? null : handle.ownerToken())
                .fencingToken(handle == null || !handle.fencingToken().isPresent() ? null : handle.fencingToken().getAsLong())
                .waitDuration(waitDuration)
                .holdDuration(holdDuration)
                .build();
    }

    /**
     * 构造失败结果。
     *
     * @param status       失败状态。
     * @param error        失败异常。
     * @param handle       锁句柄，可能为空。
     * @param waitDuration 等待耗时。
     * @param holdDuration 持锁耗时。
     * @param <T>          返回值类型。
     * @return 失败结果。
     */
    public static <T> LockResult<T> failure(
            LockStatus status,
            Throwable error,
            LockHandle handle,
            Duration waitDuration,
            Duration holdDuration
    ) {
        return LockResult.<T>builder()
                .status(status)
                .error(error)
                .handle(handle)
                .lockName(handle == null ? null : handle.lockName())
                .lockKey(handle == null ? null : handle.lockKey())
                .ownerToken(handle == null ? null : handle.ownerToken())
                .fencingToken(handle == null || !handle.fencingToken().isPresent() ? null : handle.fencingToken().getAsLong())
                .waitDuration(waitDuration)
                .holdDuration(holdDuration)
                .build();
    }

    /**
     * 是否 execute 成功。
     *
     * @return 状态为 SUCCESS 返回 true。
     */
    public boolean isSuccess() {
        return status == LockStatus.SUCCESS;
    }

    /**
     * 是否获取到了锁。
     *
     * @return 状态为 ACQUIRED 或 SUCCESS 返回 true。
     */
    public boolean isAcquired() {
        return status == LockStatus.ACQUIRED || status == LockStatus.SUCCESS;
    }

    public LockStatus getStatus() {
        return status;
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

    /**
     * LockResult 构造器。
     *
     * @param <T> 返回值类型。
     */
    public static final class Builder<T> {

        private LockStatus status;
        private T value;
        private LockHandle handle;
        private Throwable error;
        private String lockName;
        private String lockKey;
        private String ownerToken;
        private Long fencingToken;
        private Duration waitDuration;
        private Duration holdDuration;

        private Builder() {
        }

        public Builder<T> status(LockStatus status) {
            this.status = status;
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

        public LockResult<T> build() {
            return new LockResult<>(this);
        }
    }
}
