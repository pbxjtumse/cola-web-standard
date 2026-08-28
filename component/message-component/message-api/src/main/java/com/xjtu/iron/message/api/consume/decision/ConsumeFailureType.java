package com.xjtu.iron.message.api.consume.decision;

/**
 * 消费失败类型。
 *
 * <p>该枚举用于描述一次消息消费失败的主要原因，通常会出现在 {@link ConsumeDecision}
 * 或消费执行结果中，用于日志、指标、告警和问题排查。</p>
 *
 * <p>它不直接决定消息最终是 ACK、RETRY、DISCARD 还是 DEAD_LETTER，
 * 最终消费决策应由消费执行模板、异常分类器、幂等执行器或业务 Handler 共同决定。</p>
 */
public enum ConsumeFailureType {

    /**
     * 没有失败。
     *
     * <p>通常用于消费成功、幂等重复成功跳过、业务明确丢弃但流程正常结束等场景。</p>
     */
    NONE,

    /**
     * 消息解码失败。
     *
     * <p>例如 Provider 原始消息无法通过 MessageWireCodec 解码成 MessageEnvelope，
     * 或消息体格式不兼容、schemaVersion 不支持、payload 反序列化失败等。</p>
     *
     * <p>这类错误通常属于 poison message，简单重试大概率仍然失败。
     * v13 阶段可根据配置返回 RETRY 或 DISCARD，后续可以进入 DLQ。</p>
     */
    DECODE_ERROR,

    /**
     * 没有找到匹配的消费者定义。
     *
     * <p>例如 Provider 收到了消息，但本地没有对应的 ConsumerDefinition、
     * MessageHandler 或订阅配置。</p>
     *
     * <p>正常情况下应在启动阶段完成校验，避免运行时出现该错误。</p>
     */
    CONSUMER_NOT_FOUND,

    /**
     * 业务 Handler 执行失败。
     *
     * <p>例如业务方法抛出异常，或者 Handler 执行过程中出现不可预期错误。</p>
     *
     * <p>默认可以转换为 RETRY，让 Broker 后续重新投递；
     * 后续也可以通过 ConsumeExceptionClassifier 判断是否 DISCARD 或 DEAD_LETTER。</p>
     */
    HANDLER_ERROR,

    /**
     * 幂等冲突。
     *
     * <p>例如幂等记录已经处于 PROCESSING 且尚未超时，说明另一个消费者实例正在处理同一个幂等 key。</p>
     *
     * <p>这种情况不应该执行业务 Handler，通常返回 RETRY，
     * 让 Broker 后续重新投递或稍后重试。</p>
     */
    IDEMPOTENCY_CONFLICT,

    /**
     * 幂等存储异常。
     *
     * <p>例如幂等组件访问数据库、Redis 或其他存储失败，导致无法 acquire、
     * markSuccess、markFailed 或 markDiscarded。</p>
     *
     * <p>幂等开启时，幂等存储不可用不能绕过幂等直接执行业务，
     * 通常应返回 RETRY，避免重复执行业务。</p>
     */
    IDEMPOTENCY_STORAGE_ERROR,

    /**
     * 消费事务执行失败。
     *
     * <p>例如事务模板开启失败、提交失败、回滚失败，或者业务 Handler 与幂等终态更新所在事务执行异常。</p>
     *
     * <p>该错误通常意味着本次消费不能被确认为成功，应返回 RETRY。</p>
     */
    TRANSACTION_ERROR,

    /**
     * 消息确认失败。
     *
     * <p>例如业务处理和幂等 markSuccess 已经成功，但 Kafka commit offset、
     * Pulsar acknowledge 或 RocketMQ 返回消费成功前后发生异常。</p>
     *
     * <p>该错误通常不会导致业务重复生效，因为后续 Broker 重投时，
     * 幂等记录已经是 SUCCESS，可以跳过 Handler 后再次确认消息。</p>
     */
    ACK_ERROR,

    /**
     * Provider 层异常。
     *
     * <p>例如 Kafka、Pulsar、RocketMQ 客户端在消费回调、消息确认、连接状态、
     * 元数据处理或底层协议交互中出现异常。</p>
     *
     * <p>该类型用于承接具体 MQ Provider 无法归类到更细失败类型的错误。</p>
     */
    PROVIDER_ERROR,

    /**
     * 未知错误。
     *
     * <p>用于兜底表示当前失败原因无法明确分类。</p>
     *
     * <p>生产代码中应尽量避免大量落到 UNKNOWN_ERROR，
     * 如果某类错误频繁出现，应补充更明确的失败类型或异常分类规则。</p>
     */
    UNKNOWN_ERROR
}
