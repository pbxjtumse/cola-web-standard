package com.xjtu.iron.message.demo.controller;

import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import com.xjtu.iron.message.demo.dto.SendMessageRequest;
import com.xjtu.iron.message.demo.dto.SendMessageResponse;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot Demo HTTP 接口。
 *
 * <p>该 Demo 使用当前一期稳定的 MessageTemplate、MessageDestination 和 MessageEnvelope。</p>
 */
@RestController
@RequestMapping("/demo/messages")
public class MessageDemoController {

    /** 消息发送入口。 */
    private final MessageTemplate messageTemplate;

    /** 接收消息观察仓库。 */
    private final InMemoryReceivedMessageStore receivedMessageStore;

    /** 默认逻辑命名空间；不要填写 Provider 物理 namespace。 */
    private final String defaultNamespace;

    /** 默认逻辑消息名称；不要填写 persistent:// 物理 Topic。 */
    private final String defaultTopic;

    public MessageDemoController(
            MessageTemplate messageTemplate,
            InMemoryReceivedMessageStore receivedMessageStore,
            @Value("${xjtu.iron.message.demo.destination-namespace:demo}") String defaultNamespace,
            @Value("${xjtu.iron.message.demo.destination-name:message-demo-topic}") String defaultTopic) {
        this.messageTemplate = messageTemplate;
        this.receivedMessageStore = receivedMessageStore;
        this.defaultNamespace = defaultNamespace;
        this.defaultTopic = defaultTopic;
    }

    @PostMapping("/send")
    public SendMessageResponse send(@RequestBody SendMessageRequest request) {
        // Demo 中把 topic 字段当作逻辑 name 使用。
        String name = request.getTopic();
        // 未传入时使用默认逻辑名称。
        if (name == null || name.isBlank()) {
            name = defaultTopic;
        }
        // 消息类型未传入时给出 Demo 默认类型。
        String messageType = request.getEventType();
        if (messageType == null || messageType.isBlank()) {
            messageType = "DemoMessage";
        }
        // 构造逻辑目的地；真实物理 Topic 仍由路由表决定。
        MessageDestination destination = MessageDestination.of(defaultNamespace, name);
        // 构造统一消息信封。
        Map<String, Object> payload = request.getPayload() == null
                ? Map.of()
                : new LinkedHashMap<>(request.getPayload());
        // 用户消息头允许为空，进入 Builder 前先收口为明确类型，避免泛型推断发散。
        Map<String, String> headers = request.getHeaders() == null
                ? Map.of()
                : new LinkedHashMap<>(request.getHeaders());
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope
                .<Map<String, Object>>builder(messageType, payload)
                .messageKey(request.getBusinessKey())
                .headers(headers)
                .build();
        // 发送消息。
        SendResult result = messageTemplate.send(destination, envelope);
        // 返回标准结果摘要。
        return new SendMessageResponse(
                result.messageId(),
                result.providerMessageId(),
                destination.qualifiedName(),
                result.status().name());
    }

    @GetMapping("/received")
    public List<ReceivedMessageView> received() {
        // 返回接收记录。
        return receivedMessageStore.list();
    }

    @DeleteMapping("/received")
    public void clear() {
        // 清空接收记录。
        receivedMessageStore.clear();
    }

    @GetMapping("/health")
    public String health() {
        // Demo 健康检查。
        return "OK";
    }
}
