package com.xjtu.iron.distributed.lock.core.spi.response;

import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.time.Duration;
import java.util.Objects;

/**
 * Provider 加锁响应。
 *
 * <p>该对象只表达底层 Provider 的加锁事实：成功、未抢到、Provider 异常。
 * 不直接表达 API 最终状态，最终状态由 core 层根据等待策略和执行阶段统一映射。</p>
 */
public final class LockAcquireResponse {

    /** Provider 加锁状态。 */
    private final LockStatus status;

    /** 加锁成功后的租约快照。 */
    private final LockLease lease;

    /** 未获取到锁时底层锁剩余 TTL；Provider 不支持时为空。 */
    private final Duration remainingTtl;

    /** Provider 异常。 */
    private final Throwable error;

    /** Provider 额外消息。 */
    private final String message;

    private LockAcquireResponse(Builder builder) {
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.lease = builder.lease;
        this.remainingTtl = builder.remainingTtl;
        this.error = builder.error;
        this.message = builder.message;

        if (status == LockStatus.ACQUIRED && lease == null) {
            throw new IllegalArgumentException("lease must not be null when status is ACQUIRED");
        }
        if (status != LockStatus.ACQUIRED && lease != null) {
            throw new IllegalArgumentException("lease must be null when status is not ACQUIRED");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LockAcquireResponse acquired(LockLease lease) {
        return builder().status(LockStatus.ACQUIRED).lease(lease).build();
    }

    public static LockAcquireResponse notAcquired(Duration remainingTtl) {
        return builder().status(LockStatus.NOT_ACQUIRED).remainingTtl(remainingTtl).build();
    }

    public static LockAcquireResponse failed(Throwable error) {
        return builder()
                .status(LockStatus.PROVIDER_ERROR)
                .error(error)
                .message(error == null ? null : error.getMessage())
                .build();
    }

    public LockStatus getStatus() {
        return status;
    }

    public boolean isAcquired() {
        return status == LockStatus.ACQUIRED;
    }

    public boolean isNotAcquired() {
        return status == LockStatus.NOT_ACQUIRED;
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
        return status == LockStatus.PROVIDER_ERROR || error != null;
    }

    /** LockAcquireResponse 构造器。 */
    public static final class Builder {

        private LockStatus status;
        private LockLease lease;
        private Duration remainingTtl;
        private Throwable error;
        private String message;

        private Builder() {
        }

        public Builder status(LockStatus status) {
            this.status = status;
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
