package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.OperationSafety;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 重试组件 Spring Boot 配置属性。
 */
@ConfigurationProperties(prefix = "iron.retry")
public class RetryProperties {

    /**
     * 是否启用重试组件自动配置。
     */
    private boolean enabled = true;

    /**
     * 默认策略名称。
     */
    private String defaultPolicy = "default";

    /**
     * 按名称配置的重试策略。
     */
    private Map<String, PolicyProperties> policies = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(String defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Map<String, PolicyProperties> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, PolicyProperties> policies) {
        this.policies = policies == null ? new LinkedHashMap<>() : new LinkedHashMap<>(policies);
    }

    /**
     * 单个命名策略配置。
     */
    public static class PolicyProperties {

        private int maxAttempts = 3;
        private Duration maxDuration = Duration.ofSeconds(5);
        private OperationSafety operationSafety = OperationSafety.UNSPECIFIED;
        private List<String> retryOn = new ArrayList<>();
        private List<String> stopOn = new ArrayList<>();
        private BackoffProperties backoff = new BackoffProperties();

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getMaxDuration() {
            return maxDuration;
        }

        public void setMaxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
        }

        public OperationSafety getOperationSafety() {
            return operationSafety;
        }

        public void setOperationSafety(OperationSafety operationSafety) {
            this.operationSafety = operationSafety;
        }

        public List<String> getRetryOn() {
            return retryOn;
        }

        public void setRetryOn(List<String> retryOn) {
            this.retryOn = retryOn == null ? new ArrayList<>() : new ArrayList<>(retryOn);
        }

        public List<String> getStopOn() {
            return stopOn;
        }

        public void setStopOn(List<String> stopOn) {
            this.stopOn = stopOn == null ? new ArrayList<>() : new ArrayList<>(stopOn);
        }

        public BackoffProperties getBackoff() {
            return backoff;
        }

        public void setBackoff(BackoffProperties backoff) {
            this.backoff = backoff == null ? new BackoffProperties() : backoff;
        }
    }

    /**
     * 退避策略配置。
     */
    public static class BackoffProperties {

        private BackoffType type = BackoffType.NONE;
        private Duration delay = Duration.ofMillis(100);
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(2);
        private double multiplier = 2.0D;

        public BackoffType getType() {
            return type;
        }

        public void setType(BackoffType type) {
            this.type = type;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
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
    }

    /**
     * 支持的退避策略类型。
     */
    public enum BackoffType {
        NONE,
        FIXED,
        EXPONENTIAL,
        EXPONENTIAL_JITTER
    }
}
