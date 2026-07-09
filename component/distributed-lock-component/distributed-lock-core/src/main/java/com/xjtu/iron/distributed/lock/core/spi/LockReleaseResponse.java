package com.xjtu.iron.distributed.lock.core.spi;

/**
 * Provider 解锁响应。
 */
public final class LockReleaseResponse {

    /** 解锁状态。 */
    private final LockReleaseStatus status;

    /** Provider 异常。 */
    private final Throwable error;

    /** Provider 额外消息。 */
    private final String message;

    private LockReleaseResponse(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public static Builder builder() { return new Builder(); }
    public static LockReleaseResponse released() { return builder().status(LockReleaseStatus.RELEASED).build(); }
    public static LockReleaseResponse notOwner() { return builder().status(LockReleaseStatus.NOT_OWNER).build(); }
    public static LockReleaseResponse notFound() { return builder().status(LockReleaseStatus.NOT_FOUND).build(); }
    public static LockReleaseResponse failed(Throwable error) { return builder().status(LockReleaseStatus.PROVIDER_ERROR).error(error).message(error == null ? null : error.getMessage()).build(); }

    public LockReleaseStatus getStatus() { return status; }
    public Throwable getError() { return error; }
    public String getMessage() { return message; }
    public boolean isReleased() { return status == LockReleaseStatus.RELEASED; }
    public boolean isLockLost() { return status == LockReleaseStatus.NOT_OWNER || status == LockReleaseStatus.NOT_FOUND; }

    /** LockReleaseResponse 构造器。 */
    public static final class Builder {
        private LockReleaseStatus status;
        private Throwable error;
        private String message;
        private Builder() {}
        public Builder status(LockReleaseStatus status) { this.status = status; return this; }
        public Builder error(Throwable error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public LockReleaseResponse build() { return new LockReleaseResponse(this); }
    }
}
