package com.xjtu.iron.message.core.id;

import java.util.UUID;

/**
 * 使用 UUID 生成普通消息 ID。
 */
public final class UuidMessageIdGenerator implements MessageIdGenerator {

    /**
     * 生成不带连字符的 UUID 字符串。
     *
     * @return 32 位消息 ID
     */
    @Override
    public String nextId() {
        // 移除连字符可缩短消息头长度，并保持跨 Provider 可用。
        return UUID.randomUUID().toString().replace("-", "");
    }
}
