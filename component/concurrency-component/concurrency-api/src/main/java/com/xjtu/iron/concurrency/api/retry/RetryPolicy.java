package com.xjtu.iron.concurrency.api.retry;

import java.time.Duration;
import java.util.Objects;

/**
 * 异步任务重试策略占位模型。
 *
 * <p>一期只作为 {@code AsyncTask} 的元数据保留，不在并行组件内部执行复杂重试。
 * 后续建议由 governance-component 提供真正的重试执行能力，再通过 integration 接入。</p>
 */
public class RetryPolicy {

    /**
     * 是否启用重试。
     */
    private boolean enabled;

    /**
     * 最大重试次数，不包含首次执行。
     */
    private int maxAttempts;

    /**
     * 首次重试等待时间。
     */
    private Duration initialDelay = Duration.ofMillis(100);

    /**
     * 最大重试等待时间。
     */
    private Duration maxDelay = Duration.ofSeconds(3);

    /**
     * 退避倍数。1 表示固定间隔；大于 1 表示指数退避。
     */
    private double multiplier = 1.0D;

    /**
     * 语义说明，例如 FAST、BUSINESS、RPC、DB。
     */
    private String name = "none";

    /**
     * 创建不启用重试的策略。
     */
    public static RetryPolicy none() {
        RetryPolicy policy = new RetryPolicy();
        policy.enabled = false;
        policy.maxAttempts = 0;
        policy.name = "none";
        return policy;
    }

    /**
     * 创建快速重试策略占位。
     *
     * @param maxAttempts 最大重试次数，不包含首次执行
     * @param delay 每次重试的基础等待时间
     * @return 重试策略
     */
    public static RetryPolicy fast(int maxAttempts, Duration delay) {
        RetryPolicy policy = new RetryPolicy();
        policy.enabled = true;
        policy.maxAttempts = maxAttempts;
        policy.initialDelay = Objects.requireNonNull(delay, "delay must not be null");
        policy.maxDelay = delay;
        policy.multiplier = 1.0D;
        policy.name = "fast";
        return policy;
    }

    /**
     * 创建指数退避重试策略占位。
     */
    public static RetryPolicy exponentialBackoff(int maxAttempts, Duration initialDelay, Duration maxDelay, double multiplier) {
        RetryPolicy policy = new RetryPolicy();
        policy.enabled = true;
        policy.maxAttempts = maxAttempts;
        policy.initialDelay = Objects.requireNonNull(initialDelay, "initialDelay must not be null");
        policy.maxDelay = Objects.requireNonNull(maxDelay, "maxDelay must not be null");
        policy.multiplier = multiplier;
        policy.name = "exponentialBackoff";
        return policy;
    }

    /**
     * 校验重试策略配置。
     */
    public void validate() {
        if (!enabled) {
            return;
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("retry maxAttempts must be positive when retry is enabled");
        }
        if (initialDelay == null || initialDelay.isNegative()) {
            throw new IllegalArgumentException("retry initialDelay must not be negative");
        }
        if (maxDelay == null || maxDelay.isNegative()) {
            throw new IllegalArgumentException("retry maxDelay must not be negative");
        }
        if (multiplier < 1.0D) {
            throw new IllegalArgumentException("retry multiplier must be >= 1.0");
        }
    }

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

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
