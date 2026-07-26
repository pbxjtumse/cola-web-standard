package com.xjtu.iron.message.core;

/**
 * 定义消息唯一标识生成策略。
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
