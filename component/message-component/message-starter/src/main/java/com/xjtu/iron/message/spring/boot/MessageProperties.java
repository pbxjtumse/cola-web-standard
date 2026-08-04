package com.xjtu.iron.message.spring.boot;

import com.xjtu.iron.message.core.DestinationRoutingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息组件 Spring Boot 配置属性。
 *
 * <p>该类只描述 message-core 的通用配置，不直接保存 Kafka、RocketMQ、Pulsar 的原生连接参数。</p>
 *
 * <p>Provider 专属配置由各 integration 模块自己的 {@code @ConfigurationProperties} 类承载，
 * 例如 Pulsar 使用 {@code PulsarMessageProperties} 绑定 {@code xjtu.iron.message.pulsar}。</p>
 */
@ConfigurationProperties(prefix = "xjtu.iron.message")
public class MessageProperties {

    /** 是否启用消息组件自动配置。 */
    private boolean enabled = true;

    /** 默认 Provider 名称；没有 providerHint 且路由未覆盖时使用。 */
    private String provider = "pulsar";

    /** 当前应用名称；会作为消息上下文 source 的默认值。 */
    private String applicationName;

    /** 业务未显式声明 schemaVersion 时使用的默认版本。 */
    private String defaultSchemaVersion = "1";

    /** 同步发送等待 Provider 确认的默认超时。 */
    private Duration defaultConfirmTimeout = Duration.ofSeconds(15);

    /** 未命中精确路由时的处理策略；生产环境建议 STRICT。 */
    private DestinationRoutingMode routingMode = DestinationRoutingMode.STRICT;

    /** 消息体序列化配置。 */
    private Serializer serializer = new Serializer();

    /** 逻辑目的地到 Provider 物理目的地的精确路由表。 */
    private List<Route> routes = new ArrayList<>();

    /** Demo 辅助配置；生产业务可以忽略。 */
    private Demo demo = new Demo();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getDefaultSchemaVersion() {
        return defaultSchemaVersion;
    }

    public void setDefaultSchemaVersion(String defaultSchemaVersion) {
        this.defaultSchemaVersion = defaultSchemaVersion;
    }

    public Duration getDefaultConfirmTimeout() {
        return defaultConfirmTimeout;
    }

    public void setDefaultConfirmTimeout(Duration defaultConfirmTimeout) {
        this.defaultConfirmTimeout = defaultConfirmTimeout;
    }

    public DestinationRoutingMode getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(DestinationRoutingMode routingMode) {
        this.routingMode = routingMode;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes == null ? new ArrayList<>() : routes;
    }

    public Demo getDemo() {
        return demo;
    }

    public void setDemo(Demo demo) {
        this.demo = demo;
    }

    /**
     * Demo 辅助配置。
     *
     * <p>这些字段只服务 message-demo-springboot，真实业务可以不配置。</p>
     */
    public static class Demo {

        /** 是否在 Demo 启动时自动订阅消息。 */
        private boolean autoSubscribe = false;

        /** Demo 默认逻辑消息名称。 */
        private String destinationName = "message-demo-topic";

        public boolean isAutoSubscribe() {
            return autoSubscribe;
        }

        public void setAutoSubscribe(boolean autoSubscribe) {
            this.autoSubscribe = autoSubscribe;
        }

        public String getDestinationName() {
            return destinationName;
        }

        public void setDestinationName(String destinationName) {
            this.destinationName = destinationName;
        }
    }

    /**
     * 消息体序列化配置。
     *
     * <p>一期默认只自动装配 Jackson；后续可扩展 Avro、Protobuf 或 Schema Registry。</p>
     */
    public static class Serializer {

        /** 序列化器类型；当前支持 jackson。 */
        private String type = "jackson";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    /**
     * 一条逻辑目的地路由配置。
     *
     * <p>业务代码只使用 namespace + name，真正的物理 Topic 由该路由映射出来。</p>
     */
    public static class Route {

        /** 逻辑命名空间，例如 demo、trade、payment。 */
        private String namespace;

        /** 逻辑消息名称，例如 message-demo-topic、order-paid。 */
        private String name;

        /** 目标 Provider 名称，例如 pulsar、kafka、rocketmq。 */
        private String provider;

        /** Provider 物理目的地，例如 Pulsar persistent://public/default/topic。 */
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
}
