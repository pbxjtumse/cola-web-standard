package com.xjtu.iron.cola.web.model.policy;

import java.time.Duration;

public class RateLimitPolicy {

    private boolean enabled = false;

    private int limitForPeriod = 100;

    private Duration limitRefreshPeriod = Duration.ofSeconds(1);

    /**
     * 限流拿许可的等待时间。
     * 默认 0ms，拿不到直接拒绝。
     */
    private Duration timeoutDuration = Duration.ZERO;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public void setLimitForPeriod(int limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public void setLimitRefreshPeriod(Duration limitRefreshPeriod) {
        this.limitRefreshPeriod = limitRefreshPeriod;
    }

    public Duration getTimeoutDuration() {
        return timeoutDuration;
    }

    public void setTimeoutDuration(Duration timeoutDuration) {
        this.timeoutDuration = timeoutDuration;
    }
}
