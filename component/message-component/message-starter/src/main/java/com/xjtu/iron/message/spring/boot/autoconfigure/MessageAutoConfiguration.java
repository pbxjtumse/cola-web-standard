package com.xjtu.iron.message.spring.boot.autoconfigure;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.nanoid.NanoIdStringIdGenerator;
import com.xjtu.iron.foundation.id.registry.StringIdGeneratorRegistry;
import com.xjtu.iron.foundation.serialization.Serializer;
import com.xjtu.iron.foundation.serialization.jackson.JacksonJsonSerializer;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.send.reliability.DefaultReliableMessageSender;
import com.xjtu.iron.message.core.routing.DestinationRoute;
import com.xjtu.iron.message.core.routing.DestinationRouteRegistry;
import com.xjtu.iron.message.core.send.DirectMessageSender;
import com.xjtu.iron.message.core.provider.MessageProviderRegistry;
import com.xjtu.iron.message.core.send.MessageSendExecutor;
import com.xjtu.iron.message.core.send.MessageSendReliabilityOptions;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageRouteProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageSendReliabilityProperties;
import com.xjtu.iron.retry.api.execution.RetryExecutor;
import com.xjtu.iron.retry.api.policy.RetryPolicyRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.concurrent.ForkJoinPool;

/**
 * 消息组件 Spring Boot 自动配置入口。
 */
@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(prefix = "xjtu.iron.message", name = "enabled", havingValue = "true", matchIfMissing = true)
/**
 * message-component 的 Spring Boot 自动装配入口。
 *
 * <p>它负责把业务 yml 配置转换为核心组件对象，包括路由注册表、Provider 注册表、序列化器、messageId 生成器、
 * MessageTemplate 以及发送执行器。业务系统只需要引入 starter 并配置 {@code xjtu.iron.message.*} 即可获得组件能力。</p>
 *
 * <p>二期可靠发送的关键装配规则也在这里：如果关闭可靠发送，则装配 {@code DirectMessageSender}；
 * 如果开启可靠发送，则必须存在 retry-component 的 {@code RetryExecutor} 和 {@code RetryPolicyRegistry}，
 * 否则启动失败，避免配置上声称开启可靠发送、实际却静默直发。</p>
 */
public class MessageAutoConfiguration {

    /**
     * 创建默认消息体序列化器。
     *
     * @return 默认消息体序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    public Serializer messagePayloadSerializer() {
        return new JacksonJsonSerializer();
    }

    /**
     * 创建 message-core 运行参数。
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
     * 创建发送可靠性选项。
     *
     * @param properties 消息组件配置
     * @return 发送可靠性选项
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSendReliabilityOptions messageSendReliabilityOptions(MessageProperties properties) {
        MessageSendReliabilityProperties send = properties.getReliability().getSend();
        return new MessageSendReliabilityOptions(
                send.isEnabled(),
                send.getRetryPolicy(),
                send.isRetryWhenUnknown(),
                send.isIncludeReliabilityInfo());
    }

    /**
     * 创建逻辑目的地路由注册表。
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
     * 创建发送执行器。
     *
     * <p>
     * reliability.send.enabled=false 时使用一期直发执行器。
     * reliability.send.enabled=true 时必须存在 retry-component 相关 Bean。
     * </p>
     *
     * @param options 组件运行参数
     * @param reliabilityOptions 发送可靠性选项
     * @param retryExecutorProvider retry 执行器提供者
     * @param retryPolicyRegistryProvider retry 策略注册表提供者
     * @return 发送执行器
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageSendExecutor messageSendExecutor(
            MessageComponentOptions options,
            MessageSendReliabilityOptions reliabilityOptions,
            ObjectProvider<RetryExecutor> retryExecutorProvider,
            ObjectProvider<RetryPolicyRegistry> retryPolicyRegistryProvider) {
        if (!reliabilityOptions.enabled()) {
            return new DirectMessageSender(options.clock());
        }
        RetryExecutor retryExecutor = retryExecutorProvider.getIfAvailable();
        RetryPolicyRegistry retryPolicyRegistry = retryPolicyRegistryProvider.getIfAvailable();
        if (retryExecutor == null || retryPolicyRegistry == null) {
            throw new IllegalStateException(
                    "xjtu.iron.message.reliability.send.enabled=true, "
                            + "but RetryExecutor or RetryPolicyRegistry is missing. "
                            + "Please add retry-config/retry-core dependency or disable send reliability.");
        }
        return new DefaultReliableMessageSender(
                retryExecutor,
                retryPolicyRegistry,
                reliabilityOptions,
                options.clock(),
                ForkJoinPool.commonPool());
    }


    /**
     * 创建默认消息 ID 生成器。
     *
     * <p>message-component 直接复用 foundation-id。优先使用业务声明的 {@code StringIdGeneratorRegistry}
     * 中的 message/default 生成器，其次使用唯一的业务 {@code StringIdGenerator} Bean。都不存在时，使用
     * foundation-id 的 NanoId fallback，保证 demo 可以独立启动。</p>
     *
     * @return 消息 ID 生成器
     */
    @Bean("messageStringIdGenerator")
    @ConditionalOnMissingBean(name = "messageStringIdGenerator")
    public StringIdGenerator messageStringIdGenerator(ListableBeanFactory beanFactory) {
        StringIdGenerator fromRegistry = resolveFromRegistry(beanFactory);
        if (fromRegistry != null) {
            return fromRegistry;
        }
        StringIdGenerator fromBean = resolveNamedStringIdGenerator(beanFactory);
        if (fromBean != null) {
            return fromBean;
        }
        return new NanoIdStringIdGenerator();
    }

    private static StringIdGenerator resolveFromRegistry(ListableBeanFactory beanFactory) {
        String[] registryNames = beanFactory.getBeanNamesForType(StringIdGeneratorRegistry.class, false, false);
        for (String registryName : registryNames) {
            StringIdGeneratorRegistry registry = beanFactory.getBean(registryName, StringIdGeneratorRegistry.class);
            if (registry.contains("message")) {
                return registry.require("message");
            }
            if (registry.contains("default")) {
                return registry.require("default");
            }
        }
        return null;
    }

    private static StringIdGenerator resolveNamedStringIdGenerator(ListableBeanFactory beanFactory) {
        for (String preferredName : List.of("messageIdGenerator", "defaultStringIdGenerator")) {
            if (beanFactory.containsBean(preferredName)) {
                return beanFactory.getBean(preferredName, StringIdGenerator.class);
            }
        }
        return null;
    }

    /**
     * 创建统一消息模板。
     *
     * @param options 组件运行参数
     * @param providerRegistry Provider 注册表
     * @param routeRegistry 路由注册表
     * @param payloadSerializer 消息体序列化器
     * @param sendExecutor 发送执行器
     * @return 统一消息模板
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MessageProviderRegistry.class)
    public MessageTemplate messageTemplate(
            MessageComponentOptions options,
            MessageProviderRegistry providerRegistry,
            DestinationRouteRegistry routeRegistry,
            Serializer payloadSerializer,
            @Qualifier("messageStringIdGenerator")
            StringIdGenerator messageIdGenerator,
            MessageSendExecutor sendExecutor) {
        return MessageTemplate.create(
                options,
                providerRegistry,
                routeRegistry,
                payloadSerializer,
                messageIdGenerator,
                sendExecutor);
    }
}
