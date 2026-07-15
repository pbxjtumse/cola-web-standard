package com.xjtu.iron.distributed.lock.provider.redis.spring;

import com.xjtu.iron.distributed.lock.provider.redis.RedisLockKeyBuilder;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

class RedisLockKeyBuilderTest {
    @Test
    void shouldUseSameHashTagForLockAndFenceKey() {
        RedisLockKeyBuilder builder = new RedisLockKeyBuilder();
        assertEquals("iron:lock:{ns:job:1}:lock", builder.buildLockKey("ns", "job:1"));
        assertEquals("iron:lock:{ns:job:1}:fence", builder.buildFencingKey("ns", "job:1"));
    }
}
