package com.xjtu.iron.cola.web.model.policy;


import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class RetryPolicy {

    private boolean enabled = false;

    /**
     * 总尝试次数。
     * 1 = 只调用一次，不重试。
     * 2 = 首次调用 + 重试 1 次。
     */
    private int maxAttempts = 1;

    private Duration waitDuration = Duration.ofMillis(100);

    /**
     * 一期先保留字段，默认由异常映射策略控制。
     */
    private Set<String> retryExceptions = new HashSet<>();

    private Set<String> ignoreExceptions = new HashSet<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }

    public void setWaitDuration(Duration waitDuration) {
        this.waitDuration = waitDuration;
    }

    public Set<String> getRetryExceptions() {
        return retryExceptions;
    }

    public void setRetryExceptions(Set<String> retryExceptions) {
        this.retryExceptions = retryExceptions;
    }

    public Set<String> getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public void setIgnoreExceptions(Set<String> ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }
}
