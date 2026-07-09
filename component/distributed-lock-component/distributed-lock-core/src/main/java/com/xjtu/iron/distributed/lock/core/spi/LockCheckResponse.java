package com.xjtu.iron.distributed.lock.core.spi;

/**
 * Provider 持锁检查响应。
 */
public final class LockCheckResponse {

    private final LockCheckStatus status;
    private final Throwable error;
    private final String message;

    private LockCheckResponse(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        if (status == null) { throw new IllegalArgumentException("status must not be null"); }
    }

    public static Builder builder() { return new Builder(); }
    public static LockCheckResponse held() { return builder().status(LockCheckStatus.HELD).build(); }
    public static LockCheckResponse notOwner() { return builder().status(LockCheckStatus.NOT_OWNER).build(); }
    public static LockCheckResponse notFound() { return builder().status(LockCheckStatus.NOT_FOUND).build(); }
    public static LockCheckResponse failed(Throwable error) { return builder().status(LockCheckStatus.PROVIDER_ERROR).error(error).message(error == null ? null : error.getMessage()).build(); }

    public LockCheckStatus getStatus() { return status; }
    public Throwable getError() { return error; }
    public String getMessage() { return message; }
    public boolean isHeld() { return status == LockCheckStatus.HELD; }
    public boolean isLockLost() { return status == LockCheckStatus.NOT_OWNER || status == LockCheckStatus.NOT_FOUND; }

    /** LockCheckResponse 构造器。 */
    public static final class Builder {
        private LockCheckStatus status;
        private Throwable error;
        private String message;
        private Builder() {}
        public Builder status(LockCheckStatus status) { this.status = status; return this; }
        public Builder error(Throwable error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public LockCheckResponse build() { return new LockCheckResponse(this); }
    }
}
