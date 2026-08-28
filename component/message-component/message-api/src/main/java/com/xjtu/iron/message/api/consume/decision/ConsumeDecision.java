package com.xjtu.iron.message.api.consume.decision;

/**
 * 统一消费决策，表示业务处理、幂等判断和异常分类之后，Provider 应如何反馈 Broker。
 *
 * <p>它不是 Kafka/Pulsar/RocketMQ 的原生确认动作。Provider 需要把该决策映射为自己的底层语义：</p>
 * <ul>
 *   <li>Kafka：commit offset 或不提交 offset；</li>
 *   <li>Pulsar：acknowledge 或 negativeAcknowledge；</li>
 *   <li>RocketMQ 4：CONSUME_SUCCESS 或 RECONSUME_LATER。</li>
 * </ul>
 */
public enum ConsumeDecision {

    /** 消费完成，Provider 可以确认消息。 */
    ACK,

    /** 本次消费未完成，Provider 应保留或触发后续重投。 */
    RETRY,

    /** 业务明确不再处理，Provider 可以确认消息并结束这条投递。 */
    DISCARD,

    /** 进入死信；v13 只保留语义，完整 DLQ 治理放到后续消费可靠性二期。 */
    DEAD_LETTER;

    /**
     * 判断该决策对 Broker 来说是否等价于确认消息。
     *
     * @return ACK 或 DISCARD 返回 true
     */
    public boolean shouldAcknowledge() {
        return this == ACK || this == DISCARD;
    }

    /**
     * 判断该决策是否需要 Broker 后续重新投递。
     *
     * @return RETRY 返回 true
     */
    public boolean shouldRetry() {
        return this == RETRY;
    }
}
