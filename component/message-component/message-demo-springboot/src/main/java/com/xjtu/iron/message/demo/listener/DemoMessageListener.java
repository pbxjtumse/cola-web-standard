package com.xjtu.iron.message.demo.listener;

import com.xjtu.iron.message.api.MessageEnvelope;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import org.springframework.stereotype.Component;

/**
 * Demo 消息监听器占位类。
 *
 * <p>当前 Starter 尚未启用注解式监听自动注册，因此该类只保留业务处理方法，
 * 后续二期 Spring Boot Starter 稳定后再接入 @MessageListener 扫描。</p>
 */
@Component
public class DemoMessageListener {

    /** 接收消息观察仓库。 */
    private final InMemoryReceivedMessageStore store;

    public DemoMessageListener(InMemoryReceivedMessageStore store) {
        this.store = store;
    }

    /**
     * 保存收到的消息。
     *
     * @param message 消息信封
     */
    public void onMessage(MessageEnvelope<?> message) {
        // 保存消息视图。
        store.add(message);
    }
}
