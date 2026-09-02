package com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.ConsumerReliabilityMode;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.Idempotency.MessageConsumeIdempotencyProperties;


/**
 * 消费侧默认配置。
 *
 * <p>该类是 {@code xjtu.iron.message.consume.*} 的嵌套配置对象，
 * 不单独声明 {@code @ConfigurationProperties}。整个消息组件只保留
 * {@code MessageProperties} 一个根配置入口，避免多个 prefix 分散绑定。</p>
 */
public final class MessageConsumeProperties {

    /** 是否启用消费侧能力。当前主要作为预留开关，订阅仍由业务显式调用。 */
    private boolean enabled = true;

    /** 默认消费可靠性语义。 */
    private ConsumerReliabilityMode reliabilityMode = ConsumerReliabilityMode.AT_LEAST_ONCE;

    /** 消费幂等配置。 */
    private MessageConsumeIdempotencyProperties idempotency = new MessageConsumeIdempotencyProperties();

    /** 消费事务配置。 */
    private MessageConsumeTransactionProperties transaction = new MessageConsumeTransactionProperties();

    /** 未找到 Consumer 时的默认决策。 */
    private ConsumeDecision missingConsumerDecision = ConsumeDecision.RETRY;

    /** 解码失败时的默认决策。 */
    private ConsumeDecision decodeFailureDecision = ConsumeDecision.RETRY;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ConsumerReliabilityMode getReliabilityMode() {
        return reliabilityMode;
    }

    public void setReliabilityMode(ConsumerReliabilityMode reliabilityMode) {
        this.reliabilityMode = reliabilityMode == null ? ConsumerReliabilityMode.AT_LEAST_ONCE : reliabilityMode;
    }

    public MessageConsumeIdempotencyProperties getIdempotency() {
        return idempotency;
    }

    public void setIdempotency(MessageConsumeIdempotencyProperties idempotency) {
        this.idempotency = idempotency == null ? new MessageConsumeIdempotencyProperties() : idempotency;
    }

    public MessageConsumeTransactionProperties getTransaction() {
        return transaction;
    }

    public void setTransaction(MessageConsumeTransactionProperties transaction) {
        this.transaction = transaction == null ? new MessageConsumeTransactionProperties() : transaction;
    }

    public ConsumeDecision getMissingConsumerDecision() {
        return missingConsumerDecision;
    }

    public void setMissingConsumerDecision(ConsumeDecision missingConsumerDecision) {
        this.missingConsumerDecision = missingConsumerDecision == null ? ConsumeDecision.RETRY : missingConsumerDecision;
    }

    public ConsumeDecision getDecodeFailureDecision() {
        return decodeFailureDecision;
    }

    public void setDecodeFailureDecision(ConsumeDecision decodeFailureDecision) {
        this.decodeFailureDecision = decodeFailureDecision == null ? ConsumeDecision.RETRY : decodeFailureDecision;
    }
}
