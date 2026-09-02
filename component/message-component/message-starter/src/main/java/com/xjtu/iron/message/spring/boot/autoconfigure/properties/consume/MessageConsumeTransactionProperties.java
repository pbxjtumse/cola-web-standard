package com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume;

import java.time.Duration;

/**
 * 消费事务配置。
 *
 * <p>该类对应 {@code xjtu.iron.message.consume.transaction.*}，
 * 仅作为 {@code MessageProperties} 的嵌套配置对象使用。</p>
 */
public final class MessageConsumeTransactionProperties {

    /** 是否启用消费事务语义。 */
    private boolean enabled;

    /** 开启事务后是否必须存在真实事务执行器。 */
    private boolean required;

    /** 单次消费事务超时。当前作为 integration 预留配置。 */
    private Duration timeout = Duration.ofSeconds(30);

    /** 事务名称。当前作为 integration 预留配置。 */
    private String transactionName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }
}
