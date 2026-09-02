package com.xjtu.iron.message.demo.controller;

import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.publish.SendResult;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.api.publish.SendOptions;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import com.xjtu.iron.message.demo.dto.SendMessageRequest;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Demo 消息发送和消费验证接口。
 */
@RestController
@RequestMapping("/demo/messages")
public class MessageDemoController {

    private final MessageTemplate messageTemplate;
    private final InMemoryReceivedMessageStore store;

    public MessageDemoController(
            MessageTemplate messageTemplate,
            InMemoryReceivedMessageStore store) {
        this.messageTemplate = messageTemplate;
        this.store = store;
    }

    @PostMapping("/send")
    public SendResult send(
            @RequestBody SendMessageRequest request) {

        Map<String,Object> payload = request.getPayload() == null ? Map.of() : request.getPayload();

        MessageEnvelope<Map<String,Object>> envelope =
                MessageEnvelope.builder("DemoMessage", payload)
                        .messageKey(request.getBusinessKey())
                        .build();

        return messageTemplate.send(
                MessageDestination.of("demo", "message"), envelope, SendOptions.defaults());
    }

    @GetMapping("/received")
    public List<ReceivedMessageView> received() {
        return store.list();
    }
}
