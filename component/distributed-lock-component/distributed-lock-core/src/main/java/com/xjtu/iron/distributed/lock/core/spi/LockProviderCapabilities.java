package com.xjtu.iron.distributed.lock.core.spi;

/**
 * Provider 能力描述。
 *
 * <p>不同底层实现支持的锁能力不同。core 层应在执行前校验 options 与 Provider 能力是否匹配，避免运行到一半才失败。</p>
 */
public final class LockProviderCapabilities {

    /**
     * 是否支持自动续期。
     */
    private final boolean autoRenewSupported;

    /**
     * 是否支持 fencing token。
     */
    private final boolean fencingTokenSupported;

    /**
     * 是否支持 pub/sub 等待通知。
     */
    private final boolean pubSubWaitSupported;

    /**
     * 是否支持公平锁。
     */
    private final boolean fairLockSupported;

    /**
     * 是否支持可重入锁。
     */
    private final boolean reentrantSupported;

    private LockProviderCapabilities(Builder builder) {
        this.autoRenewSupported = builder.autoRenewSupported;
        this.fencingTokenSupported = builder.fencingTokenSupported;
        this.pubSubWaitSupported = builder.pubSubWaitSupported;
        this.fairLockSupported = builder.fairLockSupported;
        this.reentrantSupported = builder.reentrantSupported;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Redis 一期 Provider 推荐能力。
     *
     * @return Redis 能力描述。
     */
    public static LockProviderCapabilities redisBasic() {
        return builder()
                .autoRenewSupported(true)
                .fencingTokenSupported(true)
                .pubSubWaitSupported(false)
                .fairLockSupported(false)
                .reentrantSupported(false)
                .build();
    }

    public boolean isAutoRenewSupported() {
        return autoRenewSupported;
    }

    public boolean isFencingTokenSupported() {
        return fencingTokenSupported;
    }

    public boolean isPubSubWaitSupported() {
        return pubSubWaitSupported;
    }

    public boolean isFairLockSupported() {
        return fairLockSupported;
    }

    public boolean isReentrantSupported() {
        return reentrantSupported;
    }

    /**
     * LockProviderCapabilities 构造器。
     */
    public static final class Builder {

        private boolean autoRenewSupported;
        private boolean fencingTokenSupported;
        private boolean pubSubWaitSupported;
        private boolean fairLockSupported;
        private boolean reentrantSupported;

        private Builder() {
        }

        public Builder autoRenewSupported(boolean autoRenewSupported) {
            this.autoRenewSupported = autoRenewSupported;
            return this;
        }

        public Builder fencingTokenSupported(boolean fencingTokenSupported) {
            this.fencingTokenSupported = fencingTokenSupported;
            return this;
        }

        public Builder pubSubWaitSupported(boolean pubSubWaitSupported) {
            this.pubSubWaitSupported = pubSubWaitSupported;
            return this;
        }

        public Builder fairLockSupported(boolean fairLockSupported) {
            this.fairLockSupported = fairLockSupported;
            return this;
        }

        public Builder reentrantSupported(boolean reentrantSupported) {
            this.reentrantSupported = reentrantSupported;
            return this;
        }

        public LockProviderCapabilities build() {
            return new LockProviderCapabilities(this);
        }
    }
}
