package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

/**
 * Spring Boot 层发送可靠性配置。
 */
public class MessageSendReliabilityProperties {

    /** 是否启用发送可靠性增强。 */
    private boolean enabled = true;

    /** 使用的 retry 策略名称。 */
    private String retryPolicy = "message-send";

    /**
     * 发送结果 UNKNOWN 时是否允许继续重试。
     *
     * <p>V1 默认 false，避免重复消息。</p>
     */
    private boolean retryWhenUnknown = false;

    /** 是否在 SendResult 中暴露可靠性信息。 */
    private boolean includeReliabilityInfo = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public boolean isRetryWhenUnknown() {
        return retryWhenUnknown;
    }

    public void setRetryWhenUnknown(boolean retryWhenUnknown) {
        this.retryWhenUnknown = retryWhenUnknown;
    }

    public boolean isIncludeReliabilityInfo() {
        return includeReliabilityInfo;
    }

    public void setIncludeReliabilityInfo(boolean includeReliabilityInfo) {
        this.includeReliabilityInfo = includeReliabilityInfo;
    }
}
