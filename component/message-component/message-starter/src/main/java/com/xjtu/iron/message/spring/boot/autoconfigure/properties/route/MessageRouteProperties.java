package com.xjtu.iron.message.spring.boot.autoconfigure.properties.route;

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

    /**
     * 兼容 physical-destination 配置项。
     *
     * <p>组件内部统一使用 physicalName；该别名仅用于兼容较早的示例配置，
     * 推荐新配置统一写成 physical-name。</p>
     *
     * @return Provider 物理目的地
     */
    public String getPhysicalDestination() {
        return physicalName;
    }

    /**
     * 兼容 physical-destination 配置项。
     *
     * @param physicalDestination Provider 物理目的地
     */
    public void setPhysicalDestination(String physicalDestination) {
        this.physicalName = physicalDestination;
    }
}
