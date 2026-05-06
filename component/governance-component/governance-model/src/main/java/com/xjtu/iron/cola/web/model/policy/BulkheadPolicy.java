package com.xjtu.iron.cola.web.model.policy;

import java.time.Duration;

public class BulkheadPolicy {

    private boolean enabled = true;

    private int maxConcurrentCalls = 50;

    /**
     * 默认不等待，拿不到许可直接拒绝。
     */
    private Duration maxWaitDuration = Duration.ZERO;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public void setMaxConcurrentCalls(int maxConcurrentCalls) {
        this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public Duration getMaxWaitDuration() {
        return maxWaitDuration;
    }

    public void setMaxWaitDuration(Duration maxWaitDuration) {
        this.maxWaitDuration = maxWaitDuration;
    }
}
