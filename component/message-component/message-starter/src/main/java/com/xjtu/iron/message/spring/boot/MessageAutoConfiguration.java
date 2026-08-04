package com.xjtu.iron.message.spring.boot;

import com.xjtu.iron.message.api.MessageSerializer;
import com.xjtu.iron.message.codec.jackson.JacksonMessageSerializer;
import com.xjtu.iron.message.core.DestinationRoute;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.spi.MessageProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息组件 Spring Boot 自动配置入口。
 *
 * <p>Starter 只编排 message-core 的通用对象：</p>
 * <ul>
 *     <li>序列化器</li>
 *     <li>组件运行参数</li>
 *     <li>路由注册表</li>
 *     <li>Provider 注册表</li>
 *     <li>核心 MessageTemplate</li>
 * </ul>
 *
 * <p>具体 Provider 由各 integration 模块自动配置，例如 Pulsar 模块创建 {@code PulsarMessageProvider}。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageAutoConfiguration {

    /**
     * 创建默认消息序列化器。
     *
     * <p>业务没有显式提供 MessageSerializer Bean 时，默认使用 Jackson JSON。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSerializer messageSerializer() {
        return new JacksonMessageSerializer();
    }

    /**
     * 创建 message-core 运行参数。
     *
     * <p>这些参数不包含 Provider 原生连接参数，只控制消息组件自己的公共行为。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageComponentOptions messageComponentOptions(MessageProperties properties) {
        return new MessageComponentOptions(
                properties.getProvider(),
                properties.getApplicationName(),
                properties.getDefaultSchemaVersion(),
                properties.getDefaultConfirmTimeout(),
                properties.getRoutingMode(),
                Clock.systemUTC());
    }

    /**
     * 创建逻辑目的地路由注册表。
     *
     * <p>STRICT 模式下，没有配置在 routes 中的逻辑目的地会被拒绝，防止误投 Topic。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public DestinationRouteRegistry destinationRouteRegistry(MessageProperties properties) {
        List<DestinationRoute> routes = new ArrayList<>();
        for (MessageProperties.Route route : properties.getRoutes()) {
            routes.add(new DestinationRoute(
                    route.getNamespace(),
                    route.getName(),
                    route.getProvider(),
                    route.getPhysicalName(),
                    java.util.Map.of()));
        }
        return new DestinationRouteRegistry(routes);
    }

    /**
     * 创建 Provider 注册表。
     *
     * <p>这里收集所有已经由 integration 模块创建出来的 MessageProvider Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageProvider.class)
    public MessageProviderRegistry messageProviderRegistry(ObjectProvider<MessageProvider> providers) {
        return new MessageProviderRegistry(providers.stream().toList());
    }

    /**
     * 创建统一消息模板。
     *
     * <p>业务发送和消费注册都应使用这个稳定入口，而不是直接依赖某个 Provider。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageProviderRegistry.class)
    public MessageTemplate messageTemplate(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            MessageSerializer serializer) {
        return MessageTemplate.create(options, providerRegistry, routeRegistry, serializer);
    }
}
