package com.xjtu.iron.message.demo.listener;

import com.xjtu.iron.message.api.ConsumeDecision;
import com.xjtu.iron.message.api.ConsumerDefinition;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.MessageSubscription;
import com.xjtu.iron.message.core.MessageTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Demo 启动后的普通消息订阅器。
 *
 * <p>当前 message-starter 还没有做注解式 @MessageListener 扫描，
 * 因此 Demo 先通过 MessageTemplate 显式订阅，便于验证 Pulsar 的基础消费闭环。</p>
 */
@Component
public class DemoSubscriptionRunner implements ApplicationRunner, AutoCloseable {

    /** 消息模板，负责统一订阅生命周期。 */
    private final MessageTemplate messageTemplate;

    /** Demo 业务处理器。 */
    private final DemoMessageListener listener;

    /** 是否自动订阅。 */
    private final boolean autoSubscribe;

    /** Demo 默认逻辑消息名称。 */
    private final String destinationName;

    /** Demo 消费组，同时会映射为 Pulsar subscriptionName。 */
    private final String consumerGroup;

    /** 当前启动出来的订阅句柄。 */
    private MessageSubscription subscription;

    public DemoSubscriptionRunner(
            MessageTemplate messageTemplate,
            DemoMessageListener listener,
            @Value("${xjtu.iron.message.demo.auto-subscribe:false}") boolean autoSubscribe,
            @Value("${xjtu.iron.message.demo.destination-name:message-demo-topic}") String destinationName,
            @Value("${xjtu.iron.message.pulsar.consumer.subscription-name:message-demo-subscription}") String consumerGroup) {
        this.messageTemplate = messageTemplate;
        this.listener = listener;
        this.autoSubscribe = autoSubscribe;
        this.destinationName = destinationName;
        this.consumerGroup = consumerGroup;
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
        if (!autoSubscribe) {
            return;
        }
        // Demo 固定使用 namespace=demo，name 来自配置。
        MessageDestination destination = MessageDestination.of("demo", destinationName);
        // Java 运行期无法表达 Map<String, Object>.class，因此这里使用 Map.class。
        ConsumerDefinition<Map> definition = ConsumerDefinition.of(
                destination,
                consumerGroup,
                Map.class);
        // 启动订阅；业务成功保存后返回 SUCCESS，让 Provider ACK 或提交位点。
        this.subscription = messageTemplate.subscribe(definition, (message, context) -> {
            // 当前消费到的消息信封。
            MessageEnvelope<?> typedMessage = message;

            // 调用 Demo 自己的监听器。
            listener.onMessage(typedMessage);

            // 业务处理成功后返回 SUCCESS，Provider 才会 ACK 或提交位点。
            return ConsumeDecision.SUCCESS;
        });
    }

    /**
     * 应用关闭时释放订阅资源。
     */
    @Override
    public void close() {
        // 订阅不存在说明未启用自动订阅或启动失败。
        if (subscription == null) {
            return;
        }
        // 关闭底层 Provider 订阅。
        subscription.close();
    }
}
