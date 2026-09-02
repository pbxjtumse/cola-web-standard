package com.xjtu.iron.message.demo.store;

import com.xjtu.iron.message.demo.dto.ReceivedMessageView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Demo 接收消息存储。
 *
 * <p>只用于验证消费链路，不代表生产存储方案。</p>
 */
@Component
public class InMemoryReceivedMessageStore {

    private final List<ReceivedMessageView> messages = new CopyOnWriteArrayList<>();

    public void add(ReceivedMessageView message) {
        messages.add(message);
    }

    public List<ReceivedMessageView> list() {
        return List.copyOf(messages);
    }
}
