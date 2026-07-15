package com.xjtu.iron.distributed.lock.starter.configuration;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.provider.redis.RedisLockProvider;
import com.xjtu.iron.distributed.lock.provider.redis.RedisLockScriptExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedLockAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisDistributedLockAutoConfiguration.class, DistributedLockAutoConfiguration.class));

    @Test
    void shouldCreateClientWhenRedisProviderExists() {
        contextRunner.withBean(RedisLockScriptExecutor.class, () -> (script, keys, args) -> Arrays.asList(0L, 1L))
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisLockProvider.class);
                    assertThat(context).hasSingleBean(DistributedLockClient.class);
                });
    }
}
