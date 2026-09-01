package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import com.xjtu.iron.message.core.routing.DestinationRoutingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * message-component 的 Spring Boot 根配置属性。
 *
 * <p>该类承接 {@code xjtu.iron.message.*} 下的公共配置，包括默认 Provider、路由列表、demo 配置
 * 以及二期发送可靠性配置。payload 序列化统一由 foundation-serialization 的 Serializer Bean 承接。</p>
 */
@ConfigurationProperties(prefix = "xjtu.iron.message")
public class MessageProperties {

    /**
     * 是否启用消息组件自动配置。
     */
    private boolean enabled = true;

    /**
     * 默认 Provider 名称。
     *
     * <p>没有 providerHint 且路由未覆盖 Provider 时使用。</p>
     */
    private String provider = "pulsar";

    /**
     * 当前应用名称。
     *
     * <p>发送消息时会作为 MessageContext.source 的默认值。</p>
     */
    private String applicationName;

    /**
     * 业务未显式声明 schemaVersion 时使用的默认版本。
     */
    private String defaultSchemaVersion = "1";

    /**
     * 同步发送等待 Provider 确认的默认超时。
     */
    private Duration defaultConfirmTimeout = Duration.ofSeconds(15);

    /**
     * 未命中精确路由时的处理策略。
     *
     * <p>生产环境建议使用 STRICT，避免逻辑名称拼错后误投物理 Topic。</p>
     */
    private DestinationRoutingMode routingMode = DestinationRoutingMode.STRICT;

    /**
     * 可靠性增强配置。
     */
    private Reliability reliability = new Reliability();

    /**
     * 消费侧默认配置。
     *
     * <p>原来工程中已经存在 MessageConsumeProperties，但没有挂到根配置对象，
     * 导致 xjtu.iron.message.consume.* 实际不会被 Spring Boot 绑定。这里补齐根入口。</p>
     */
    private MessageConsumeProperties consume = new MessageConsumeProperties();

    /**
     * 逻辑目的地到 Provider 物理目的地的精确路由表。
     */
    private List<MessageRouteProperties> routes = new ArrayList<>();

    /**
     * Demo 辅助配置；生产业务可以忽略。
     */
    private MessageDemoProperties demo = new MessageDemoProperties();

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

    public Reliability getReliability() {
        return reliability;
    }

    public void setReliability(Reliability reliability) {
        this.reliability = reliability == null ? new Reliability() : reliability;
    }

    public MessageConsumeProperties getConsume() {
        return consume;
    }

    public void setConsume(MessageConsumeProperties consume) {
        this.consume = consume == null ? new MessageConsumeProperties() : consume;
    }

    public List<MessageRouteProperties> getRoutes() {
        return routes;
    }

    public void setRoutes(List<MessageRouteProperties> routes) {
        this.routes = routes == null ? new ArrayList<>() : routes;
    }

    public MessageDemoProperties getDemo() {
        return demo;
    }

    public void setDemo(MessageDemoProperties demo) {
        this.demo = demo == null ? new MessageDemoProperties() : demo;
    }

    /**
     * 消息组件可靠性增强配置分组。
     */
    public static class Reliability {

        /**
         * 发送可靠性配置。
         */
        private MessageSendReliabilityProperties send = new MessageSendReliabilityProperties();

        public MessageSendReliabilityProperties getSend() {
            return send;
        }

        public void setSend(MessageSendReliabilityProperties send) {
            this.send = send == null ? new MessageSendReliabilityProperties() : send;
        }
    }
}
