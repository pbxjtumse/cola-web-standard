package com.xjtu.iron.message.spring.boot.autoconfigure;

import com.xjtu.iron.message.api.MessageSerializer;
import com.xjtu.iron.message.codec.jackson.JacksonMessageSerializer;
import com.xjtu.iron.message.core.DestinationRoute;
import com.xjtu.iron.message.core.DestinationRouteRegistry;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.MessageProviderRegistry;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageRouteProperties;
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
import java.util.Map;

/**
 * 消息组件 Spring Boot 自动配置入口。
 *
 * <p>Starter 只编排 message-core 的通用对象：</p>
 * <ul>
 *     <li>消息体序列化器</li>
 *     <li>组件运行参数</li>
 *     <li>逻辑目的地路由表</li>
 *     <li>Provider 注册表</li>
 *     <li>核心 MessageTemplate</li>
 * </ul>
 *
 * <p>具体 Provider 由各 integration 模块自动配置，例如 Pulsar 模块创建 PulsarMessageProvider。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageAutoConfiguration {

    /**
     * 创建默认消息体序列化器。
     *
     * <p>业务没有显式提供 MessageSerializer Bean 时，默认使用 Jackson JSON。</p>
     *
     * <p>如果后续需要统一复用 foundation-component，只需要在业务应用或独立 codec 模块中
     * 提供一个 MessageSerializer Bean，当前 Bean 会因为 ConditionalOnMissingBean 自动让位。</p>
     *
     * @return 默认消息体序列化器
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
     *
     * @param properties 通用配置
     * @return 组件运行参数
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
     *
     * @param properties 通用配置
     * @return 路由注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public DestinationRouteRegistry destinationRouteRegistry(MessageProperties properties) {
        List<DestinationRoute> routes = new ArrayList<>();
        for (MessageRouteProperties route : properties.getRoutes()) {
            routes.add(new DestinationRoute(
                    route.getNamespace(),
                    route.getName(),
                    route.getProvider(),
                    route.getPhysicalName(),
                    Map.of()));
        }
        return new DestinationRouteRegistry(routes);
    }

    /**
     * 创建 Provider 注册表。
     *
     * <p>这里收集所有已经由 integration 模块创建出来的 MessageProvider Bean。</p>
     *
     * @param providers Provider Bean 集合
     * @return Provider 注册表
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
     *
     * @param options 组件运行参数
     * @param providerRegistry Provider 注册表
     * @param routeRegistry 路由注册表
     * @param serializer 消息体序列化器
     * @return 统一消息模板
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
