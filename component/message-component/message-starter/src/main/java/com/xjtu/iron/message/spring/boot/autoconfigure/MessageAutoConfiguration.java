package com.xjtu.iron.message.spring.boot.autoconfigure;

import com.xjtu.iron.message.api.codec.MessageSerializer;
import com.xjtu.iron.message.core.MessageComponentOptions;
import com.xjtu.iron.message.core.codec.JacksonMessageSerializer;
import com.xjtu.iron.message.core.send.reliability.DefaultReliableMessageSender;
import com.xjtu.iron.message.core.routing.DestinationRoute;
import com.xjtu.iron.message.core.routing.DestinationRouteRegistry;
import com.xjtu.iron.message.core.send.DirectMessageSender;
import com.xjtu.iron.message.core.provider.MessageProviderRegistry;
import com.xjtu.iron.message.core.send.MessageSendExecutor;
import com.xjtu.iron.message.core.send.MessageSendReliabilityOptions;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.core.id.FoundationMessageIdGenerator;
import com.xjtu.iron.message.core.id.MessageIdGenerator;
import com.xjtu.iron.message.core.id.UuidMessageIdGenerator;
import com.xjtu.iron.message.spi.MessageProvider;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageRouteProperties;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageSendReliabilityProperties;
import com.xjtu.iron.retry.api.execution.RetryExecutor;
import com.xjtu.iron.retry.api.policy.RetryPolicyRegistry;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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
    public MessageSerializer messageSerializer() {
        return new JacksonMessageSerializer();
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
     * <p>
     * 生产工程建议提供自己的 MessageIdGenerator Bean，
     * 例如使用 FoundationMessageIdGenerator 适配 foundation-component 的统一 ID 能力。
     * 没有显式 Bean 时才使用 UUID fallback，保证 demo 可以独立启动。
     * </p>
     *
     * @return 消息 ID 生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public MessageIdGenerator messageIdGenerator(ListableBeanFactory beanFactory) {
        Optional<Supplier<String>> foundationIdSupplier = detectFoundationIdSupplier(beanFactory);
        if (foundationIdSupplier.isPresent()) {
            return FoundationMessageIdGenerator.from(foundationIdSupplier.get());
        }
        return new UuidMessageIdGenerator();
    }

    /**
     * 尝试自动发现 foundation-component 提供的字符串 ID 生成器。
     *
     * <p>
     * message-starter 不直接依赖 foundation-id 的具体 API，避免基础组件接口
     * 重构时强制修改 message-core。这里采用保守的反射适配：只识别
     * 包名或接口名明显属于 foundation-id 的 Bean，并且只调用无参的
     * nextId / nextStringId / generateId / next 方法。
     * </p>
     */
    private static Optional<Supplier<String>> detectFoundationIdSupplier(ListableBeanFactory beanFactory) {
        String[] beanNames = beanFactory.getBeanNamesForType(Object.class, false, false);
        for (String beanName : beanNames) {
            Class<?> beanType = beanFactory.getType(beanName, false);
            if (!looksLikeFoundationIdGenerator(beanType)) {
                continue;
            }
            Method method = findIdMethod(beanType);
            if (method == null) {
                continue;
            }
            return Optional.of(() -> invokeIdMethod(beanFactory.getBean(beanName), method));
        }
        return Optional.empty();
    }

    private static boolean looksLikeFoundationIdGenerator(Class<?> beanType) {
        if (beanType == null) {
            return false;
        }
        String className = beanType.getName();
        if (className.startsWith("com.xjtu.iron.foundation.id.")) {
            return true;
        }
        if (className.contains(".foundation.id.")) {
            return true;
        }
        for (Class<?> interfaceType : beanType.getInterfaces()) {
            String interfaceName = interfaceType.getName();
            if (interfaceName.startsWith("com.xjtu.iron.foundation.id.")) {
                return true;
            }
        }
        return false;
    }

    private static Method findIdMethod(Class<?> beanType) {
        String[] methodNames = {"nextId", "nextStringId", "generateId", "next"};
        for (String methodName : methodNames) {
            try {
                Method method = beanType.getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 尝试下一个候选方法名。
            }
        }
        return null;
    }

    private static String invokeIdMethod(Object bean, Method method) {
        try {
            Object id = method.invoke(bean);
            if (id == null) {
                throw new IllegalStateException("foundation id generator returned null id");
            }
            String value = id.toString().trim();
            if (value.isEmpty()) {
                throw new IllegalStateException("foundation id generator returned blank id");
            }
            return value;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("cannot access foundation id generator method", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException("foundation id generator invocation failed", cause);
        }
    }

    /**
     * 创建统一消息模板。
     *
     * @param options 组件运行参数
     * @param providerRegistry Provider 注册表
     * @param routeRegistry 路由注册表
     * @param serializer 消息体序列化器
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
            MessageSerializer serializer,
            MessageIdGenerator messageIdGenerator,
            MessageSendExecutor sendExecutor) {
        return MessageTemplate.create(
                options,
                providerRegistry,
                routeRegistry,
                serializer,
                messageIdGenerator,
                sendExecutor);
    }
}
