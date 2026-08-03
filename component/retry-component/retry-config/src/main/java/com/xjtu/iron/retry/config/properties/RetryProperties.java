package com.xjtu.iron.retry.config.properties;

import com.xjtu.iron.retry.api.policy.OperationSafety;
import com.xjtu.iron.retry.api.policy.RetryFailureCategory;
import com.xjtu.iron.retry.api.policy.RetrySafetyMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 绑定 retry-component 的 Spring Boot 外部化配置。 */
@ConfigurationProperties(prefix = "iron.retry")
public class RetryProperties {

    /** 是否启用全部自动配置。 */
    private boolean enabled = true;
    /** 是否将核心事件桥接到 Spring ApplicationEvent。 */
    private boolean publishSpringEvents = true;
    /** 是否在存在 Micrometer 时注册指标监听器。 */
    private boolean metricsEnabled = true;
    /** 命名策略原始配置。 */
    private Map<String, PolicyProperties> policies = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPublishSpringEvents() {
        return publishSpringEvents;
    }

    public void setPublishSpringEvents(boolean publishSpringEvents) {
        this.publishSpringEvents = publishSpringEvents;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public Map<String, PolicyProperties> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, PolicyProperties> policies) {
        this.policies = policies == null ? new LinkedHashMap<>() : policies;
    }

    /**
     * 描述一个命名策略的原始配置。
     *
     * <p>包装类型和可空列表用于区分“没有覆盖父策略”和“显式覆盖或清空父策略”。</p>
     */
    public static class PolicyProperties {

        /** 可选父策略名称。 */
        private String basePolicy;
        /** 可选最大尝试次数覆盖。 */
        private Integer maxAttempts;
        /** 可选最大时长覆盖。 */
        private Duration maxDuration;
        /** 可选操作安全级别覆盖。 */
        private OperationSafety operationSafety;
        /** 可选安全模式覆盖。 */
        private RetrySafetyMode safetyMode;
        /** 可选 cause 遍历开关覆盖。 */
        private Boolean traverseCauses;
        /** 可选 cause 最大遍历深度覆盖。 */
        private Integer maxCauseDepth;
        /** 配置文件中 retry-on 共享的失败分类。 */
        private RetryFailureCategory retryFailureCategory;
        /** 配置文件中 retry-on 共享的稳定失败码。 */
        private String retryFailureCode;
        /** 可选可重试异常类名列表；空列表表示显式清空。 */
        private List<String> retryOn;
        /** 可选正常停止异常类名列表；空列表表示显式清空。 */
        private List<String> stopOn;
        /** 可选立即中止异常类名列表；空列表表示显式清空。 */
        private List<String> abortOn;
        /** 可选退避配置。 */
        private BackoffProperties backoff;

        public String getBasePolicy() {
            return basePolicy;
        }

        public void setBasePolicy(String basePolicy) {
            this.basePolicy = basePolicy;
        }

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
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

        public RetrySafetyMode getSafetyMode() {
            return safetyMode;
        }

        public void setSafetyMode(RetrySafetyMode safetyMode) {
            this.safetyMode = safetyMode;
        }

        public Boolean getTraverseCauses() {
            return traverseCauses;
        }

        public void setTraverseCauses(Boolean traverseCauses) {
            this.traverseCauses = traverseCauses;
        }

        public Integer getMaxCauseDepth() {
            return maxCauseDepth;
        }

        public void setMaxCauseDepth(Integer maxCauseDepth) {
            this.maxCauseDepth = maxCauseDepth;
        }

        public RetryFailureCategory getRetryFailureCategory() {
            return retryFailureCategory;
        }

        public void setRetryFailureCategory(RetryFailureCategory retryFailureCategory) {
            this.retryFailureCategory = retryFailureCategory;
        }

        public String getRetryFailureCode() {
            return retryFailureCode;
        }

        public void setRetryFailureCode(String retryFailureCode) {
            this.retryFailureCode = retryFailureCode;
        }

        public List<String> getRetryOn() {
            return retryOn;
        }

        public void setRetryOn(List<String> retryOn) {
            this.retryOn = retryOn;
        }

        public List<String> getStopOn() {
            return stopOn;
        }

        public void setStopOn(List<String> stopOn) {
            this.stopOn = stopOn;
        }

        public List<String> getAbortOn() {
            return abortOn;
        }

        public void setAbortOn(List<String> abortOn) {
            this.abortOn = abortOn;
        }

        public BackoffProperties getBackoff() {
            return backoff;
        }

        public void setBackoff(BackoffProperties backoff) {
            this.backoff = backoff;
        }
    }

    /** 描述一个策略的退避参数覆盖。 */
    public static class BackoffProperties {

        /** 可选退避类型。 */
        private BackoffType type;
        /** 可选固定等待时长。 */
        private Duration delay;
        /** 可选指数退避初始时长。 */
        private Duration initialDelay;
        /** 可选指数退避最大时长。 */
        private Duration maxDelay;
        /** 可选指数增长倍数。 */
        private Double multiplier;

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

        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Double multiplier) {
            this.multiplier = multiplier;
        }
    }

    /** 定义配置文件支持的退避策略类型。 */
    public enum BackoffType {
        /** 不等待。 */
        NONE,
        /** 固定等待。 */
        FIXED,
        /** 指数退避。 */
        EXPONENTIAL,
        /** 指数退避加全抖动。 */
        EXPONENTIAL_FULL_JITTER
    }
}
