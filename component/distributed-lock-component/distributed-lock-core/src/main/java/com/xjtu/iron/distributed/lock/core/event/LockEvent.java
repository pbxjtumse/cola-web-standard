package com.xjtu.iron.distributed.lock.core.event;

import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * 分布式锁事件。
 *
 * <p>
 * LockEvent 用于记录锁生命周期中的关键动作，例如加锁成功、加锁失败、
 * 续期失败、锁丢失、释放失败、业务执行失败等。
 * </p>
 *
 * <p>
 * LockEvent 是过程记录，不一定等价于最终 LockResult。
 * 例如 watchdog 续期失败可以先产生 LOCK_LOST 事件，
 * 最终 execute 是否返回 LOCK_LOST，要看 failOnLockLost 等选项。
 * </p>
 */
public final class LockEvent {

    /**
     * 事件类型。
     */
    private final LockEventType eventType;

    /**
     * 事件发生阶段。
     */
    private final LockStage stage;

    /**
     * 当前对应的状态。
     *
     * <p>
     * 某些过程事件可能没有明确的最终状态，此字段允许为空。
     * </p>
     */
    private final LockStatus status;

    /**
     * 命名空间。
     */
    private final String namespace;

    /**
     * 业务锁名称。
     */
    private final String lockName;

    /**
     * 底层实际 key / path。
     */
    private final String lockKey;

    /**
     * Provider 名称。
     */
    private final String providerName;

    /**
     * owner token。
     */
    private final String ownerToken;

    /**
     * fencing token。
     */
    private final Long fencingToken;

    /**
     * 异常信息。
     */
    private final Throwable error;

    /**
     * 事件时间。
     */
    private final Instant timestamp;

    private LockEvent(Builder builder) {
        this.eventType = Objects.requireNonNull(builder.eventType, "eventType must not be null");
        this.stage = builder.stage;
        this.status = builder.status;
        this.namespace = builder.namespace;
        this.lockName = builder.lockName;
        this.lockKey = builder.lockKey;
        this.providerName = builder.providerName;
        this.ownerToken = builder.ownerToken;
        this.fencingToken = builder.fencingToken;
        this.error = builder.error;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public LockEventType eventType() {
        return eventType;
    }

    public LockStage stage() {
        return stage;
    }

    public LockStatus status() {
        return status;
    }

    public String namespace() {
        return namespace;
    }

    public String lockName() {
        return lockName;
    }

    public String lockKey() {
        return lockKey;
    }

    public String providerName() {
        return providerName;
    }

    public String ownerToken() {
        return ownerToken;
    }

    public Long fencingToken() {
        return fencingToken;
    }

    public Throwable error() {
        return error;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public static final class Builder {

        private LockEventType eventType;

        private LockStage stage;

        private LockStatus status;

        private String namespace;

        private String lockName;

        private String lockKey;

        private String providerName;

        private String ownerToken;

        private Long fencingToken;

        private Throwable error;

        private Instant timestamp;

        private Builder() {
        }

        public Builder eventType(LockEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder stage(LockStage stage) {
            this.stage = stage;
            return this;
        }

        public Builder status(LockStatus status) {
            this.status = status;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder lockKey(String lockKey) {
            this.lockKey = lockKey;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder ownerToken(String ownerToken) {
            this.ownerToken = ownerToken;
            return this;
        }

        public Builder fencingToken(Long fencingToken) {
            this.fencingToken = fencingToken;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public LockEvent build() {
            return new LockEvent(this);
        }
    }
}