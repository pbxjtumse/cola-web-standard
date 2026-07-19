package com.xjtu.iron.distributed.lock.starter.configuration;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.provider.jdbc.fencing.JdbcFencingTokenConstants;
import com.xjtu.iron.distributed.lock.starter.health.DistributedLockHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DistributedLockAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisDistributedLockAutoConfiguration.class,
                    JdbcFencingTokenAutoConfiguration.class,
                    DistributedLockAutoConfiguration.class,
                    DistributedLockActuatorAutoConfiguration.class));

    @Test
    void shouldCreateClientWhenRedisProviderExists() {
        contextRunner.withBean(RedisLockScriptExecutor.class,
                        () -> (script, keys, args) -> Arrays.asList(0L, 1L))
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisLockProvider.class);
                    assertThat(context).hasSingleBean(DistributedLockClient.class);
                    assertThat(context).hasSingleBean(DistributedLockHealthIndicator.class);
                });
    }

    @Test
    void shouldBindDefaultOptionsFromProperties() {
        contextRunner.withBean(RedisLockScriptExecutor.class,
                        () -> (script, keys, args) -> Arrays.asList(0L, 1L))
                .withPropertyValues(
                        "xjtu.iron.distributed-lock.namespace=demo",
                        "xjtu.iron.distributed-lock.lease-time=45s",
                        "xjtu.iron.distributed-lock.wait-time=2s",
                        "xjtu.iron.distributed-lock.auto-renew=true",
                        "xjtu.iron.distributed-lock.renew-interval=15s",
                        "xjtu.iron.distributed-lock.max-renew-time=2m",
                        "xjtu.iron.distributed-lock.fencing-required=true",
                        "xjtu.iron.distributed-lock.fail-on-lock-lost=false")
                .run(context -> {
                    LockOptions options = context.getBean("distributedLockDefaultOptions", LockOptions.class);
                    assertThat(options.getNamespace()).isEqualTo("demo");
                    assertThat(options.getLeaseTime()).isEqualTo(Duration.ofSeconds(45));
                    assertThat(options.getWaitTime()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(options.isAutoRenew()).isTrue();
                    assertThat(options.getRenewInterval()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(options.getMaxRenewTime()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(options.isFencingRequired()).isTrue();
                    assertThat(options.isFailOnLockLost()).isFalse();
                });
    }

    @Test
    void shouldRegisterJdbcSequenceFencingProvider() {
        contextRunner
                .withBean(RedisLockScriptExecutor.class,
                        () -> (script, keys, args) -> Arrays.asList(0L, 1L))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withPropertyValues(
                        "xjtu.iron.distributed-lock.fencing.jdbc.enabled=true",
                        "xjtu.iron.distributed-lock.fencing-token-provider-name=jdbc-sequence")
                .run(context -> {
                    assertThat(context).hasBean("jdbcSequenceFencingTokenProvider");
                    FencingTokenProviderRegistry registry = context.getBean(FencingTokenProviderRegistry.class);
                    assertThat(registry.providerNames()).contains(JdbcFencingTokenConstants.PROVIDER_NAME);
                    assertThat(registry.defaultProvider()).isPresent();
                    assertThat(registry.defaultProvider().orElseThrow().providerName())
                            .isEqualTo(JdbcFencingTokenConstants.PROVIDER_NAME);
                });
    }

    @Test
    void shouldNotCreateRedisProviderWhenRedisDisabled() {
        contextRunner.withBean(RedisLockScriptExecutor.class,
                        () -> (script, keys, args) -> Arrays.asList(0L, 1L))
                .withPropertyValues("xjtu.iron.distributed-lock.redis.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisLockProvider.class);
                    assertThat(context).doesNotHaveBean(DistributedLockClient.class);
                });
    }

    @Test
    void shouldNotCreateClientWhenNoLockProviderExists() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(RedisLockProvider.class);
            assertThat(context).doesNotHaveBean(DistributedLockClient.class);
        });
    }
}
