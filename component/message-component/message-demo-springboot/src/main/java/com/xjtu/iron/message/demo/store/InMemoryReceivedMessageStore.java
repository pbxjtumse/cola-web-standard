package com.xjtu.iron.message.demo.store;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Demo 使用的内存接收消息仓库。
 *
 * <p>只用于页面或接口观察消费结果，不提供持久化能力。</p>
 */
@Component
public class InMemoryReceivedMessageStore {

    /** 最多保留的消息数量。 */
    private static final int MAX_SIZE = 500;

    /** 内存消息列表。 */
    private final LinkedList<ReceivedMessageView> messages = new LinkedList<>();

    /**
     * 追加一条接收消息。
     *
     * @param message 消息信封
     */
    public synchronized void add(MessageEnvelope<?> message) {
        add(message, null);
    }

    /**
     * 追加一条带消费上下文的接收消息。
     *
     * @param message 消息信封
     * @param context 消费上下文
     */
    public synchronized void add(MessageEnvelope<?> message, ConsumeContext context) {
        // 超过最大容量时删除最早消息。
        if (messages.size() >= MAX_SIZE) {
            messages.removeFirst();
        }
        // 保存当前消息视图。
        messages.addLast(new ReceivedMessageView(
                context == null ? null : context.providerName(),
                context == null ? null : context.physicalDestination(),
                context == null ? null : context.consumerGroup(),
                context == null ? null : context.providerMessageId(),
                message.messageId(),
                message.messageKey(),
                message.messageType(),
                message.payload(),
                message.headers().asMap(),
                Instant.now()));
    }

    /**
     * 返回接收消息快照。
     *
     * @return 消息列表
     */
    public synchronized List<ReceivedMessageView> list() {
        // 返回副本，避免外部修改内部列表。
        return new ArrayList<>(messages);
    }

    /**
     * 按 Provider 返回接收消息快照。
     *
     * @param providerName Provider 名称
     * @return 消息列表
     */
    public synchronized List<ReceivedMessageView> listByProvider(String providerName) {
        String normalized = normalizeProvider(providerName);
        List<ReceivedMessageView> result = new ArrayList<>();
        for (ReceivedMessageView message : messages) {
            if (normalized.equals(normalizeProvider(message.getProviderName()))) {
                result.add(message);
            }
        }
        return result;
    }

    /**
     * 按 Provider 统计接收数量。
     *
     * @return Provider 到数量的映射
     */
    public synchronized Map<String, Long> countByProvider() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReceivedMessageView message : messages) {
            String providerName = normalizeProvider(message.getProviderName());
            counts.put(providerName, counts.getOrDefault(providerName, 0L) + 1L);
        }
        return counts;
    }

    /**
     * 清空内存消息。
     */
    public synchronized void clear() {
        // 删除全部缓存记录。
        messages.clear();
    }

    private static String normalizeProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return "unknown";
        }
        return providerName.trim().toLowerCase(Locale.ROOT);
    }
}
