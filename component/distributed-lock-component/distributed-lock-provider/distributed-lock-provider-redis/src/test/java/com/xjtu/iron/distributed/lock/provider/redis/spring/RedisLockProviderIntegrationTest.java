package com.xjtu.iron.distributed.lock.provider.redis.spring;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.status.LockCheckStatus;
import com.xjtu.iron.distributed.lock.core.spi.status.LockReleaseStatus;
import com.xjtu.iron.distributed.lock.core.spi.status.LockRenewStatus;
import com.xjtu.iron.distributed.lock.provider.redis.RedisLockProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RedisLockProviderIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private static RedisLockProvider provider;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        provider = new RedisLockProvider(new StringRedisTemplateRedisLockScriptExecutor(redisTemplate));
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void acquireRenewCheckAndReleaseShouldUseLuaAtomically() {
        LockAcquireRequest acquireRequest = LockAcquireRequest.builder()
                .lockName("integration:job:1")
                .ownerToken("owner-a")
                .options(LockOptions.noWait(Duration.ofSeconds(5)))
                .build();

        LockAcquireResponse acquired = provider.acquire(acquireRequest);
        assertTrue(acquired.isAcquired());
        assertNotNull(acquired.getLease());

        LockAcquireResponse busy = provider.acquire(LockAcquireRequest.builder()
                .lockName("integration:job:1")
                .ownerToken("owner-b")
                .options(LockOptions.noWait(Duration.ofSeconds(5)))
                .build());
        assertTrue(busy.isNotAcquired());
        assertNotNull(busy.getRemainingTtl());

        assertEquals(LockCheckStatus.HELD, provider.check(LockCheckRequest.fromLease(acquired.getLease())).getStatus());
        assertEquals(LockRenewStatus.RENEWED, provider.renew(LockRenewRequest.fromLease(acquired.getLease())).getStatus());
        assertEquals(LockReleaseStatus.RELEASED, provider.release(LockReleaseRequest.fromLease(acquired.getLease())).getStatus());
        assertEquals(LockCheckStatus.NOT_FOUND, provider.check(LockCheckRequest.fromLease(acquired.getLease())).getStatus());
    }
}
