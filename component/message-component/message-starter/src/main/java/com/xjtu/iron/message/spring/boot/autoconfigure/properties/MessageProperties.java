package com.xjtu.iron.message.spring.boot.autoconfigure.properties;

import com.xjtu.iron.message.core.routing.DestinationRoutingMode;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.consume.MessageConsumeProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.reliability.MessageReliabilityProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.route.MessageRouteProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.serializer.MessageSerializerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * message-component 的 Spring Boot 根配置属性。
 *
 * <p>整个组件只保留这一个 {@code @ConfigurationProperties} 入口，prefix 固定为
 * {@code xjtu.iron.message}。发送、消费、可靠性、路由、序列化等子配置全部作为普通
 * Java 嵌套对象挂在该根对象下面，避免多个配置类各自声明 prefix 后造成绑定入口分散。</p>
 */
@ConfigurationProperties(prefix = "xjtu.iron.message")
public final class MessageProperties {

    /** 是否启用消息组件自动配置。 */
    private boolean enabled = true;

    /** 默认 Provider 名称；未指定 providerHint 且路由未覆盖 Provider 时使用。 */
    private String provider = "pulsar";

    /** 当前应用名称；发送消息时作为默认 source。 */
    private String applicationName;

    /** 业务未显式声明 schemaVersion 时使用的默认版本。 */
    private String defaultSchemaVersion = "1";

    /** 同步发送等待 Provider 确认的默认超时。 */
    private Duration defaultConfirmTimeout = Duration.ofSeconds(15);

    /** 未命中精确路由时的处理策略。 */
    private DestinationRoutingMode routingMode = DestinationRoutingMode.STRICT;

    /** 消息体序列化配置。 */
    private MessageSerializerProperties serializer = new MessageSerializerProperties();

    /** 可靠性增强配置。 */
    private MessageReliabilityProperties reliability = new MessageReliabilityProperties();

    /** 消费侧默认配置。 */
    private MessageConsumeProperties consume = new MessageConsumeProperties();

    /** 逻辑目的地到 Provider 物理目的地的精确路由表。 */
    private List<MessageRouteProperties> routes = new ArrayList<>();

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
        this.defaultConfirmTimeout = defaultConfirmTimeout == null ? Duration.ofSeconds(15) : defaultConfirmTimeout;
    }

    public DestinationRoutingMode getRoutingMode() {
        return routingMode;
    }

    public void setRoutingMode(DestinationRoutingMode routingMode) {
        this.routingMode = routingMode == null ? DestinationRoutingMode.STRICT : routingMode;
    }

    public MessageSerializerProperties getSerializer() {
        return serializer;
    }

    public void setSerializer(MessageSerializerProperties serializer) {
        this.serializer = serializer == null ? new MessageSerializerProperties() : serializer;
    }

    public MessageReliabilityProperties getReliability() {
        return reliability;
    }

    public void setReliability(MessageReliabilityProperties reliability) {
        this.reliability = reliability == null ? new MessageReliabilityProperties() : reliability;
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
}
