package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import java.time.Duration;

/** 消费事务配置属性。 */
public final class MessageConsumeTransactionProperties {
    private boolean enabled;
    private boolean required;
    private Duration timeout = Duration.ofSeconds(30);
    private String transactionName;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public String getTransactionName() { return transactionName; }
    public void setTransactionName(String transactionName) { this.transactionName = transactionName; }
}
