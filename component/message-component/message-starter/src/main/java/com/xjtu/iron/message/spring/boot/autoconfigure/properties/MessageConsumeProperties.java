package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import com.xjtu.iron.message.api.consume.ConsumerReliabilityMode;
import com.xjtu.iron.message.api.consume.ConsumeDecision;

/** 全局消费默认配置。 */
public final class MessageConsumeProperties {
    private boolean enabled = true;
    private ConsumerReliabilityMode reliabilityMode = ConsumerReliabilityMode.AT_LEAST_ONCE;
    private MessageConsumeIdempotencyProperties idempotency = new MessageConsumeIdempotencyProperties();
    private MessageConsumeTransactionProperties transaction = new MessageConsumeTransactionProperties();
    private ConsumeDecision missingConsumerDecision = ConsumeDecision.RETRY;
    private ConsumeDecision decodeFailureDecision = ConsumeDecision.RETRY;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public ConsumerReliabilityMode getReliabilityMode() { return reliabilityMode; }
    public void setReliabilityMode(ConsumerReliabilityMode reliabilityMode) { this.reliabilityMode = reliabilityMode; }
    public MessageConsumeIdempotencyProperties getIdempotency() { return idempotency; }
    public void setIdempotency(MessageConsumeIdempotencyProperties idempotency) { this.idempotency = idempotency; }
    public MessageConsumeTransactionProperties getTransaction() { return transaction; }
    public void setTransaction(MessageConsumeTransactionProperties transaction) { this.transaction = transaction; }
    public ConsumeDecision getMissingConsumerDecision() { return missingConsumerDecision; }
    public void setMissingConsumerDecision(ConsumeDecision missingConsumerDecision) { this.missingConsumerDecision = missingConsumerDecision; }
    public ConsumeDecision getDecodeFailureDecision() { return decodeFailureDecision; }
    public void setDecodeFailureDecision(ConsumeDecision decodeFailureDecision) { this.decodeFailureDecision = decodeFailureDecision; }
}
