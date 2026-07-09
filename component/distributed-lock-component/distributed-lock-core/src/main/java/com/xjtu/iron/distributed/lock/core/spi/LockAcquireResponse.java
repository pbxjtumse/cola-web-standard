package com.xjtu.iron.distributed.lock.core.spi;

import java.time.Duration;

/**
 * Provider 加锁响应。
 */
public final class LockAcquireResponse {

    /** 是否加锁成功。 */
    private final boolean acquired;

    /** 加锁成功后的租约。 */
    private final LockLease lease;

    /** 加锁失败时底层锁剩余 TTL，Provider 不支持时为空。 */
    private final Duration remainingTtl;

    /** Provider 异常。 */
    private final Throwable error;

    /** Provider 额外消息。 */
    private final String message;

    private LockAcquireResponse(Builder builder) {
        this.acquired = builder.acquired;
        this.lease = builder.lease;
        this.remainingTtl = builder.remainingTtl;
        this.error = builder.error;
        this.message = builder.message;
        if (acquired && lease == null) {
            throw new IllegalArgumentException("lease must not be null when acquired is true");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LockAcquireResponse acquired(LockLease lease) {
        return builder().acquired(true).lease(lease).build();
    }

    public static LockAcquireResponse notAcquired(Duration remainingTtl) {
        return builder().acquired(false).remainingTtl(remainingTtl).build();
    }

    public static LockAcquireResponse failed(Throwable error) {
        return builder().acquired(false).error(error).message(error == null ? null : error.getMessage()).build();
    }

    public boolean isAcquired() {
        return acquired;
    }

    public LockLease getLease() {
        return lease;
    }

    public Duration getRemainingTtl() {
        return remainingTtl;
    }

    public Throwable getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasError() {
        return error != null;
    }

    /** LockAcquireResponse 构造器。 */
    public static final class Builder {

        private boolean acquired;
        private LockLease lease;
        private Duration remainingTtl;
        private Throwable error;
        private String message;

        private Builder() {
        }

        public Builder acquired(boolean acquired) {
            this.acquired = acquired;
            return this;
        }

        public Builder lease(LockLease lease) {
            this.lease = lease;
            return this;
        }

        public Builder remainingTtl(Duration remainingTtl) {
            this.remainingTtl = remainingTtl;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public LockAcquireResponse build() {
            return new LockAcquireResponse(this);
        }
    }
}
