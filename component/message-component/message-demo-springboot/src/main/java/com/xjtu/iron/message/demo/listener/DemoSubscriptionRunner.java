package com.xjtu.iron.message.demo.listener;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.ConsumerDefinition;
import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.consume.handler.MessageSubscription;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Demo 启动后的普通消息订阅器。
 *
 * <p>当前 message-starter 还没有做注解式 @MessageListener 扫描，
 * 因此 Demo 先通过 MessageTemplate 显式订阅，便于验证多 Provider 的基础消费闭环。</p>
 */
@Component
/**
 * Demo 自动订阅启动器。
 *
 * <p>应用启动后，如果配置开启 auto-subscribe，它会按照 demo 配置为指定 Provider 建立订阅，
 * 这样发送接口调用后可以立刻在内存 received store 中看到消费结果。</p>
 */
public class DemoSubscriptionRunner implements ApplicationRunner, AutoCloseable {

    /** 消息模板，负责统一订阅生命周期。 */
    private final MessageTemplate messageTemplate;

    /** Demo 业务处理器。 */
    private final DemoMessageListener listener;

    /** 通用消息配置。 */
    private final MessageProperties properties;

    /** 当前启动出来的订阅句柄集合。 */
    private final List<MessageSubscription> subscriptions = new ArrayList<>();

    public DemoSubscriptionRunner(
            MessageTemplate messageTemplate,
            DemoMessageListener listener,
            MessageProperties properties) {
        this.messageTemplate = messageTemplate;
        this.listener = listener;
        this.properties = properties;
    }

    /**
     * 应用启动后按配置创建订阅。
     *
     * @param args 启动参数
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(ApplicationArguments args) {
        // 未启用自动订阅时直接跳过，避免启动阶段连接外部 Broker。
        if (!properties.getDemo().isAutoSubscribe()) {
            return;
        }
        // Demo 使用配置中的逻辑 namespace + name，物理 Topic 仍由 routes 决定。
        String namespace = properties.getDemo().getDestinationNamespace();
        String name = properties.getDemo().getDestinationName();
        String consumerGroup = properties.getDemo().getConsumerGroup();

        // 并行验证模式下，为每个 Provider 单独创建一个订阅。
        for (String providerName : enabledProviders()) {
            MessageDestination destination = MessageDestination
                    .of(namespace, name)
                    .withProviderHint(providerName);
            ConsumerDefinition<Map> definition = ConsumerDefinition.of(
                    destination,
                    consumerGroup,
                    Map.class);
            MessageSubscription subscription = messageTemplate.subscribe(definition, (message, context) -> {
                // 当前消费到的消息信封。
                MessageEnvelope<?> typedMessage = message;

                // 调用 Demo 自己的监听器，并保存 Provider 上下文。
                listener.onMessage(typedMessage, context);

                // 业务处理成功后返回 SUCCESS，Provider 才会 ACK 或提交位点。
                return ConsumeDecision.ACK;
            });
            subscriptions.add(subscription);
        }
    }

    /**
     * 应用关闭时释放订阅资源。
     */
    @Override
    public void close() {
        // 逐个关闭底层 Provider 订阅。
        for (MessageSubscription subscription : subscriptions) {
            if (subscription != null) {
                subscription.close();
            }
        }
        subscriptions.clear();
    }

    private Set<String> enabledProviders() {
        Set<String> providers = new LinkedHashSet<>();
        List<String> configured = properties.getDemo().getProviders();
        if (configured != null) {
            for (String provider : configured) {
                String normalized = normalizeProvider(provider);
                if (normalized != null) {
                    providers.add(normalized);
                }
            }
        }
        if (providers.isEmpty()) {
            providers.add(normalizeProvider(properties.getProvider()));
        }
        return providers;
    }

    private static String normalizeProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return null;
        }
        return providerName.trim().toLowerCase(Locale.ROOT);
    }
}
