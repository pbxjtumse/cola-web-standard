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
    public SendResult send(@RequestBody SendMessageRequest request) {
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope.builder(
                        request.getEventType() == null ? "DemoMessage" : request.getEventType(),
                        request.getPayload() == null ?  Map.<String, Object>of() : request.getPayload())
                .messageKey(request.getBusinessKey())
                .headers(request.getHeaders())
                .build();

        return messageTemplate.send(
                MessageDestination.of("demo", request.getTopic() == null ? "message" : request.getTopic()),
                envelope,
                SendOptions.defaults());
    }

    @GetMapping("/received")
    public List<ReceivedMessageView> received() {
        return store.list();
    }
}
