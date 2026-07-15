package com.xjtu.iron.distributed.lock.provider.redis;

import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockCheckRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockReleaseRequest;
import com.xjtu.iron.distributed.lock.core.spi.request.LockRenewRequest;
import com.xjtu.iron.distributed.lock.core.spi.response.LockAcquireResponse;
import com.xjtu.iron.distributed.lock.core.spi.status.LockAcquireStatus;
import com.xjtu.iron.distributed.lock.core.spi.status.LockCheckStatus;
import com.xjtu.iron.distributed.lock.core.spi.status.LockReleaseStatus;
import com.xjtu.iron.distributed.lock.core.spi.status.LockRenewStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class RedisLockProviderIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void acquireRenewCheckReleaseShouldWorkAgainstRealRedis() {
        RedisLockProvider provider = new RedisLockProvider(new StringRedisTemplateRedisLockScriptExecutor(template()));
        LockAcquireResponse acquire = provider.acquire(LockAcquireRequest.builder()
                .lockName("it:job:1")
                .ownerToken("token-1")
                .options(LockOptions.noWait(Duration.ofSeconds(30)))
                .build());
        assertEquals(LockAcquireStatus.ACQUIRED, acquire.getStatus());
        LockLease lease = acquire.getLease();
        assertNotNull(lease);
        assertEquals(LockCheckStatus.HELD, provider.check(LockCheckRequest.fromLease(lease)).getStatus());
        assertEquals(LockRenewStatus.RENEWED, provider.renew(LockRenewRequest.fromLease(lease)).getStatus());
        assertEquals(LockReleaseStatus.RELEASED, provider.release(LockReleaseRequest.fromLease(lease)).getStatus());
        assertEquals(LockCheckStatus.NOT_FOUND, provider.check(LockCheckRequest.fromLease(lease)).getStatus());
    }

    private StringRedisTemplate template() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        return template;
    }
}
