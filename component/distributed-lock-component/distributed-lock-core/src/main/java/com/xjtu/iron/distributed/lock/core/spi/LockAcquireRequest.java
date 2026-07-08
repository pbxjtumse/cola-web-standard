package com.xjtu.iron.distributed.lock.core.spi;

import com.xjtu.iron.distributed.lock.api.LockOptions;

import java.util.Objects;

/**
 * 底层 Provider 加锁请求。
 */
public final class LockAcquireRequest {

    /**
     * 业务锁名称。
     */
    private final String lockName;

    /**
     * 底层真实锁 key。
     */
    private final String lockKey;

    /**
     * fencing token 使用的底层 key。
     *
     * <p>Redis Provider 中通常为 {@code lockKey + ":fence"}，用于 INCR 生成单调递增版本号。
     * 如果不需要 fencing，可以为空。</p>
     */
    private final String fencingKey;

    /**
     * 本次加锁请求生成的 ownerToken。
     *
     * <p>该 token 在加锁前由 core 层生成，Provider 加锁成功后应把它写入底层锁记录。</p>
     */
    private final String ownerToken;

    /**
     * 锁选项。
     */
    private final LockOptions options;

    private LockAcquireRequest(Builder builder) {
        this.lockName = requireText(builder.lockName, "lockName");
        this.lockKey = requireText(builder.lockKey, "lockKey");
        this.fencingKey = builder.fencingKey;
        this.ownerToken = requireText(builder.ownerToken, "ownerToken");
        this.options = Objects.requireNonNull(builder.options, "options must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public String getLockName() {
        return lockName;
    }

    public String getLockKey() {
        return lockKey;
    }

    public String getFencingKey() {
        return fencingKey;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public LockOptions getOptions() {
        return options;
    }

    /**
     * LockAcquireRequest 构造器。
     */
    public static final class Builder {

        private String lockName;
        private String lockKey;
        private String fencingKey;
        private String ownerToken;
        private LockOptions options;

        private Builder() {
        }

        public Builder lockName(String lockName) {
            this.lockName = lockName;
            return this;
        }

        public Builder lockKey(String lockKey) {
            this.lockKey = lockKey;
            return this;
        }

        public Builder fencingKey(String fencingKey) {
            this.fencingKey = fencingKey;
            return this;
        }

        public Builder ownerToken(String ownerToken) {
            this.ownerToken = ownerToken;
            return this;
        }

        public Builder options(LockOptions options) {
            this.options = options;
            return this;
        }

        public LockAcquireRequest build() {
            return new LockAcquireRequest(this);
        }
    }
}
