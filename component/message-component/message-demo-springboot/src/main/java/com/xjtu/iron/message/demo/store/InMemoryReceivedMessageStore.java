package com.xjtu.iron.message.demo.store;

import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Demo 使用的内存接收消息仓库。
 *
 * <p>只用于页面或接口观察消费结果，不提供持久化能力。</p>
 */
@Component
public class InMemoryReceivedMessageStore {

    /** 最多保留的消息数量。 */
    private static final int MAX_SIZE = 100;

    /** 内存消息列表。 */
    private final LinkedList<ReceivedMessageView> messages = new LinkedList<>();

    /**
     * 追加一条接收消息。
     *
     * @param message 消息信封
     */
    public synchronized void add(MessageEnvelope<?> message) {
        // 超过最大容量时删除最早消息。
        if (messages.size() >= MAX_SIZE) {
            messages.removeFirst();
        }
        // 保存当前消息视图。
        messages.addLast(new ReceivedMessageView(
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
     * 清空内存消息。
     */
    public synchronized void clear() {
        // 删除全部缓存记录。
        messages.clear();
    }
}
