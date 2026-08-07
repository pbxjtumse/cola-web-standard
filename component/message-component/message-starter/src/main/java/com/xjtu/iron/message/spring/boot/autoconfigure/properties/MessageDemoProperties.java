package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo 辅助配置。
 *
 * <p>这些字段只服务 message-demo-springboot，真实业务应用可以完全不配置。</p>
 */
public class MessageDemoProperties {

    /**
     * 是否在 Demo 启动时自动订阅消息。
     */
    private boolean autoSubscribe = false;

    /**
     * Demo 默认逻辑命名空间。
     */
    private String destinationNamespace = "demo";

    /**
     * Demo 默认逻辑消息名称。
     */
    private String destinationName = "message-demo-topic";

    /**
     * Demo 默认消费组。
     */
    private String consumerGroup = "message-demo-consumer-group";

    /**
     * Demo 并行验证时启用的 Provider 列表。
     *
     * <p>这里的名称必须和 MessageProvider.name() 保持一致，例如 kafka、pulsar、rocketmq。</p>
     */
    private List<String> providers = new ArrayList<>(List.of("kafka", "pulsar", "rocketmq"));

    public boolean isAutoSubscribe() {
        return autoSubscribe;
    }

    public void setAutoSubscribe(boolean autoSubscribe) {
        this.autoSubscribe = autoSubscribe;
    }

    public String getDestinationNamespace() {
        return destinationNamespace;
    }

    public void setDestinationNamespace(String destinationNamespace) {
        this.destinationNamespace = destinationNamespace;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers == null ? new ArrayList<>() : providers;
    }
}
