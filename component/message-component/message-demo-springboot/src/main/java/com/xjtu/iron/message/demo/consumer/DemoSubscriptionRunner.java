package com.xjtu.iron.message.demo.consumer;

import com.xjtu.iron.message.api.consume.decision.ConsumeDecision;
import com.xjtu.iron.message.api.consume.definition.ConsumerDefinition;
import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.api.consume.handler.MessageSubscription;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Demo 消费订阅验证入口。
 *
 * <p>验证 Provider -> Adapter -> ConsumeExecutor -> Handler 全链路。</p>
 */
@Component
public class DemoSubscriptionRunner {

    private final MessageTemplate messageTemplate;
    private final InMemoryReceivedMessageStore store;
    private MessageSubscription subscription;

    public DemoSubscriptionRunner(
            MessageTemplate messageTemplate,
            InMemoryReceivedMessageStore store) {
        this.messageTemplate = messageTemplate;
        this.store = store;
    }

    @PostConstruct
    public void start() {
        ConsumerDefinition<Object> definition = ConsumerDefinition.of(
                MessageDestination.of("demo", "message"),
                "message-demo-consumer-group",
                Object.class);

        subscription = messageTemplate.subscribe(
                definition,
                (message, context) -> {
                    store.add(new ReceivedMessageView(
                            message.messageId(),
                            null,
                            message.messageType(),
                            message.payload(),
                            Map.of(),
                            Instant.now()));
                    return ConsumeDecision.ACK;
                });
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
        }
    }
}
