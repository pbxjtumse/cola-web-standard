package com.xjtu.iron.message.api;

/**
 * 表示业务逻辑目的地及其指定的消息 Provider。
 *
 * @param providerName Provider 名称，例如 kafka、rocketmq、pulsar
 * @param logicalName 逻辑目的地名称；第一版直接映射为 Topic
 */
public record MessageDestination(String providerName, String logicalName) {

    /**
     * 创建消息目的地。
     *
     * @param providerName Provider 名称
     * @param logicalName 逻辑目的地名称
     * @return 消息目的地
     */
    public static MessageDestination of(String providerName, String logicalName) {
        // 使用静态工厂提升调用处的可读性。
        return new MessageDestination(providerName, logicalName);
    }
}
