package com.xjtu.iron.message.demo.listener;

import com.xjtu.iron.message.api.consume.ConsumeContext;
import com.xjtu.iron.message.api.model.MessageEnvelope;
import com.xjtu.iron.message.demo.store.InMemoryReceivedMessageStore;
import org.springframework.stereotype.Component;

/**
 * Demo 消息监听器占位类。
 *
 * <p>当前 Starter 尚未启用注解式监听自动注册，因此该类只保留业务处理方法，
 * 后续二期 Spring Boot Starter 稳定后再接入 @MessageListener 扫描。</p>
 */
@Component
/**
 * Demo 消费监听器，负责把消费到的统一消息写入内存存储，便于通过 HTTP 接口观察消费结果。
 *
 * <p>它的职责不是执行业务逻辑，而是帮助验证 Provider 入站消息是否能被 core 正确解码，
 * 并确认 Kafka、Pulsar、RocketMQ 三个 Provider 的消费链路都能回到统一模型。</p>
 */
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

    /**
     * 保存收到的消息和消费上下文。
     *
     * @param message 消息信封
     * @param context 消费上下文
     */
    public void onMessage(MessageEnvelope<?> message, ConsumeContext context) {
        // 保存消息视图。
        store.add(message, context);
    }
}
