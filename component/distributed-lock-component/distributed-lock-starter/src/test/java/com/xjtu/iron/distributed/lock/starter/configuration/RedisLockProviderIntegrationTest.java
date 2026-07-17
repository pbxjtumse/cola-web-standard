package com.xjtu.iron.distributed.lock.starter.configuration;

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
import com.xjtu.iron.distributed.lock.starter.redis.StringRedisTemplateRedisLockScriptExecutor;
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

/**
 * Redis Lua + Spring Data Redis 适配层集成测试。
 *
 * <p>
 * Spring Data Redis 适配器位于 starter 模块，因此真实 Redis 集成测试也放在 starter 测试中，
 * provider-redis 模块只测试纯 Provider 语义和 Lua 返回值映射。
 * </p>
 */
@Testcontainers(disabledWithoutDocker = true)
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
    @Test
    void redisIncrFencingTokenShouldIncreaseAfterReleaseAndReacquire() {
        LockOptions options = LockOptions.builder()
                .namespace("integration")
                .leaseTime(Duration.ofSeconds(5))
                .waitTime(Duration.ZERO)
                .fencingRequired(true)
                .build();

        LockAcquireResponse first = provider.acquire(LockAcquireRequest.builder()
                .lockName("fencing:job:1")
                .ownerToken("owner-first")
                .options(options)
                .build());
        assertTrue(first.isAcquired());
        assertTrue(first.getLease().fencingToken().isPresent());
        long firstToken = first.getLease().fencingToken().getAsLong();
        assertEquals(LockReleaseStatus.RELEASED,
                provider.release(LockReleaseRequest.fromLease(first.getLease())).getStatus());

        LockAcquireResponse second = provider.acquire(LockAcquireRequest.builder()
                .lockName("fencing:job:1")
                .ownerToken("owner-second")
                .options(options)
                .build());
        assertTrue(second.isAcquired());
        assertTrue(second.getLease().fencingToken().isPresent());
        long secondToken = second.getLease().fencingToken().getAsLong();

        assertTrue(secondToken > firstToken,
                "fencing token must increase across successful acquisitions");
        assertEquals(LockReleaseStatus.RELEASED,
                provider.release(LockReleaseRequest.fromLease(second.getLease())).getStatus());
    }

}
