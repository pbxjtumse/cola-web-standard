package com.xjtu.iron.message.demo.controller;

import com.xjtu.iron.message.api.model.MessageDestination;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.api.publish.SendResult;
import com.xjtu.iron.message.core.MessageTemplate;
import com.xjtu.iron.message.demo.dto.MultiSendMessageResponse;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import com.xjtu.iron.message.demo.dto.SendMessageRequest;
import com.xjtu.iron.message.demo.dto.SendMessageResponse;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import com.xjtu.iron.message.spring.boot.autoconfigure.properties.MessageProperties;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Spring Boot Demo HTTP 接口。
 *
 * <p>该 Demo 使用当前一期稳定的 MessageTemplate、MessageDestination 和 MessageEnvelope。</p>
 */
@RestController
@RequestMapping("/demo/messages")
/**
 * message-component 演示工程的 HTTP 入口。
 *
 * <p>这个 Controller 不代表生产业务接口，它用于验证组件能力：单 Provider 发送、三 Provider 并行发送、
 * 消费结果查询、内存接收记录清理等。二期可靠发送接入后，响应中会额外展示 retryStatus、attempts、retryId 等信息，
 * 方便直接判断当前请求是否真的走到了可靠发送链路。</p>
 */
public class MessageDemoController {

    /** 消息发送入口。 */
    private final MessageTemplate messageTemplate;

    /** 接收消息观察仓库。 */
    private final InMemoryReceivedMessageStore receivedMessageStore;

    /** 消息组件配置。 */
    private final MessageProperties properties;

    public MessageDemoController(
            MessageTemplate messageTemplate,
            InMemoryReceivedMessageStore receivedMessageStore,
            MessageProperties properties) {
        this.messageTemplate = messageTemplate;
        this.receivedMessageStore = receivedMessageStore;
        this.properties = properties;
    }

    /**
     * 使用默认 Provider 发送一条消息。
     */
    @PostMapping("/send")
    public SendMessageResponse send(@RequestBody SendMessageRequest request) {
        String providerName = normalizeProvider(properties.getProvider());
        return sendToProvider(providerName, request, null);
    }

    /**
     * 使用指定 Provider 发送一条消息。
     *
     * <p>例如：/demo/messages/send/kafka、/demo/messages/send/pulsar、/demo/messages/send/rocketmq。</p>
     */
    @PostMapping("/send/{providerName}")
    public SendMessageResponse sendToProvider(
            @PathVariable String providerName,
            @RequestBody SendMessageRequest request) {
        return sendToProvider(normalizeProvider(providerName), request, null);
    }

    /**
     * 并行向配置中的全部 Provider 发送一条逻辑相同的消息。
     *
     * <p>每个 Provider 都会生成独立的 messageId，公共 batchId 通过 header 关联。</p>
     */
    @PostMapping("/send/all")
    public MultiSendMessageResponse sendAll(@RequestBody SendMessageRequest request) {
        Instant startedAt = Instant.now();
        String batchId = UUID.randomUUID().toString().replace("-", "");
        List<CompletableFuture<SendMessageResponse>> futures = new ArrayList<>();
        for (String providerName : enabledProviders()) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> sendToProvider(providerName, request, batchId)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SendMessageResponse> results = new ArrayList<>();
        for (CompletableFuture<SendMessageResponse> future : futures) {
            results.add(future.join());
        }
        return new MultiSendMessageResponse(batchId, startedAt, Instant.now(), results);
    }

    /**
     * 返回全部接收记录。
     */
    @GetMapping("/received")
    public List<ReceivedMessageView> received() {
        return receivedMessageStore.list();
    }

    /**
     * 按 Provider 返回接收记录。
     */
    @GetMapping("/received/{providerName}")
    public List<ReceivedMessageView> receivedByProvider(@PathVariable String providerName) {
        return receivedMessageStore.listByProvider(providerName);
    }

    /**
     * 返回各 Provider 的接收数量。
     */
    @GetMapping("/received-summary")
    public Map<String, Long> receivedSummary() {
        return receivedMessageStore.countByProvider();
    }

    /**
     * 清空接收记录。
     */
    @DeleteMapping("/received")
    public void clear() {
        receivedMessageStore.clear();
    }

    /**
     * Demo 健康检查。
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    private SendMessageResponse sendToProvider(
            String providerName,
            SendMessageRequest request,
            String batchId) {
        try {
            String name = logicalName(request);
            MessageDestination destination = MessageDestination
                    .of(properties.getDemo().getDestinationNamespace(), name)
                    .withProviderHint(providerName);
            MessageEnvelope<Map<String, Object>> envelope = buildEnvelope(request, providerName, batchId);
            SendResult result = messageTemplate.send(destination, envelope);
            return SendMessageResponse.from(result);
        } catch (RuntimeException exception) {
            return SendMessageResponse.failed(
                    providerName,
                    logicalTopic(request),
                    exception.getMessage());
        }
    }

    private MessageEnvelope<Map<String, Object>> buildEnvelope(
            SendMessageRequest request,
            String providerName,
            String batchId) {
        String messageType = request.getEventType();
        if (messageType == null || messageType.isBlank()) {
            messageType = "DemoMessage";
        }
        Map<String, Object> payload = request.getPayload() == null
                ? Map.of()
                : new LinkedHashMap<>(request.getPayload());
        Map<String, String> headers = request.getHeaders() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.getHeaders());
        headers.put("demo-provider-target", providerName);
        if (batchId != null && !batchId.isBlank()) {
            headers.put("demo-broadcast-batch-id", batchId);
        }
        return MessageEnvelope
                .<Map<String, Object>>builder(messageType, payload)
                .messageKey(request.getBusinessKey())
                .headers(headers)
                .build();
    }

    private String logicalName(SendMessageRequest request) {
        String name = request.getTopic();
        if (name == null || name.isBlank()) {
            name = properties.getDemo().getDestinationName();
        }
        return name;
    }

    private String logicalTopic(SendMessageRequest request) {
        return properties.getDemo().getDestinationNamespace() + ":" + logicalName(request);
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
