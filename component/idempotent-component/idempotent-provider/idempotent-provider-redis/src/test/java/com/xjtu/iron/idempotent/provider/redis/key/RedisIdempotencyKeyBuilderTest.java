package com.xjtu.iron.idempotent.provider.redis.key;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisIdempotencyKeyBuilderTest {
    @Test
    void buildsStableHashTag() {
        assertEquals("iron:idempotency:{order:req-1}", new RedisIdempotencyKeyBuilder("iron:idempotency").build("order", "req-1"));
    }
}
