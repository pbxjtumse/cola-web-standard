package com.xjtu.iron.message.core.consume.idempotency;

import java.util.UUID;

/**
 * 生成本次消费执行的 ownerToken。
 */
public final class MessageIdempotencyOwnerTokenGenerator {
    public String nextToken() {
        return "message-consume-" + UUID.randomUUID().toString().replace("-", "");
    }
}
