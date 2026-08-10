package com.xjtu.iron.distributed.lock.provider.redisson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedissonLockKeyBuilderTest {

    @Test
    void shouldBuildNamespacedClusterFriendlyKey() {
        RedissonLockKeyBuilder builder = new RedissonLockKeyBuilder("iron:lock:redisson:");
        assertEquals("iron:lock:redisson:{demo:order-1001}",
                builder.buildLockKey("demo", "order-1001"));
    }
}
