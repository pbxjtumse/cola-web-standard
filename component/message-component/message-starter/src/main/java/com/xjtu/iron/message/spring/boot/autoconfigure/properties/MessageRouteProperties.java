package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

/**
 * 一条逻辑目的地到 Provider 物理目的地的路由配置。
 *
 * <p>业务代码只使用 namespace + name 这样的稳定逻辑名称，
 * 真正的 Kafka Topic、RocketMQ Topic 或 Pulsar Topic 由这里映射。</p>
 */
public class MessageRouteProperties {

    /**
     * 逻辑命名空间。
     *
     * <p>例如 demo、trade、payment。</p>
     */
    private String namespace;

    /**
     * 逻辑消息名称。
     *
     * <p>例如 message-demo-topic、order-paid。</p>
     */
    private String name;

    /**
     * 目标 Provider 名称。
     *
     * <p>例如 pulsar、kafka、rocketmq。</p>
     */
    private String provider;

    /**
     * Provider 物理目的地。
     *
     * <p>例如 Pulsar 的 persistent://public/default/message-demo-topic。</p>
     */
    private String physicalName;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public void setPhysicalName(String physicalName) {
        this.physicalName = physicalName;
    }
}
