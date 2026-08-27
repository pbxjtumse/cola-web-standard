package com.xjtu.iron.message.core.id;

/**
 * message-component 内部使用的消息 ID 生成接口。
 *
 * <p>消息组件保留自己的领域接口，避免 core 代码到处直接依赖 foundation-id 的具体类型。
 * 默认实现可以是 UUID，生产体系中可以通过 {@code FoundationMessageIdGenerator} 适配统一 ID 组件。</p>
 */
@FunctionalInterface
public interface MessageIdGenerator {

    /**
     * 生成新的消息唯一标识。
     *
     * @return 新消息 ID
     */
    String nextId();
}
