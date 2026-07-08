package com.xjtu.iron.distributed.lock.core.spi;

import java.time.Instant;

/**
 * Provider 续期响应。
 */
public final class LockRenewResponse {

    /**
     * 续期状态。
     */
    private final LockRenewStatus status;

    /**
     * 续期后的本地估算过期时间。
     */
    private final Instant newExpireAt;

    /**
     * Provider 异常。
     */
    private final Throwable error;

    /**
     * Provider 额外消息。
     */
    private final String message;

    private LockRenewResponse(Builder builder) {
        this.status = builder.status;
        this.newExpireAt = builder.newExpireAt;
        this.error = builder.error;
        this.message = builder.message;
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LockRenewResponse renewed(Instant newExpireAt) {
        return builder().status(LockRenewStatus.RENEWED).newExpireAt(newExpireAt).build();
    }

    public static LockRenewResponse notOwner() {
        return builder().status(LockRenewStatus.NOT_OWNER).build();
    }

    public static LockRenewResponse notFound() {
        return builder().status(LockRenewStatus.NOT_FOUND).build();
    }

    public static LockRenewResponse failed(Throwable error) {
        return builder().status(LockRenewStatus.PROVIDER_ERROR).error(error).build();
    }

    public LockRenewStatus getStatus() {
        return status;
    }

    public Instant getNewExpireAt() {
        return newExpireAt;
    }

    public Throwable getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRenewed() {
        return status == LockRenewStatus.RENEWED;
    }

    public boolean isLockLost() {
        return status == LockRenewStatus.NOT_OWNER || status == LockRenewStatus.NOT_FOUND;
    }

    /**
     * LockRenewResponse 构造器。
     */
    public static final class Builder {

        private LockRenewStatus status;
        private Instant newExpireAt;
        private Throwable error;
        private String message;

        private Builder() {
        }

        public Builder status(LockRenewStatus status) {
            this.status = status;
            return this;
        }

        public Builder newExpireAt(Instant newExpireAt) {
            this.newExpireAt = newExpireAt;
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

        public LockRenewResponse build() {
            return new LockRenewResponse(this);
        }
    }
}
