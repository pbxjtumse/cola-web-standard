package com.xjtu.iron.retry.config.autoconfigure;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;
import com.xjtu.iron.foundation.id.registry.StringIdGeneratorRegistry;
import com.xjtu.iron.foundation.test.id.FixedStringIdGenerator;
import com.xjtu.iron.foundation.time.ClockProvider;
import com.xjtu.iron.retry.api.execution.RetryExecution;
import com.xjtu.iron.retry.api.execution.RetryExecutor;
import com.xjtu.iron.retry.api.execution.RetryStatus;
import com.xjtu.iron.retry.api.policy.RetryPolicy;
import com.xjtu.iron.retry.api.policy.RetryPolicyRegistry;
import com.xjtu.iron.retry.core.time.RetryClock;
import com.xjtu.iron.retry.core.time.RetrySleeper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证核心 Spring Boot 自动配置和策略继承。
 */
class RetryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class));

    @Test
    void shouldCreateExecutorAndResolveInheritedPolicy() {
        contextRunner
                .withPropertyValues(
                        "iron.retry.publish-spring-events=false",
                        "iron.retry.policies.base.max-attempts=4",
                        "iron.retry.policies.base.max-duration=3s",
                        "iron.retry.policies.base.retry-on[0]="
                                + IOException.class.getName(),
                        "iron.retry.policies.base.retry-failure-category=TRANSIENT",
                        "iron.retry.policies.base.max-cause-depth=8",
                        "iron.retry.policies.base.backoff.type=FIXED",
                        "iron.retry.policies.base.backoff.delay=10ms",
                        "iron.retry.policies.remote.base-policy=base",
                        "iron.retry.policies.remote.max-duration=2s"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RetryExecutor.class);
                    assertThat(context).hasSingleBean(RetryClock.class);
                    assertThat(context).hasSingleBean(ClockProvider.class);
                    assertThat(context).hasSingleBean(RetrySleeper.class);
                    assertThat(context).hasBean(
                            RetryAutoConfiguration.RETRY_ID_GENERATOR_BEAN_NAME
                    );
                    RetryPolicyRegistry registry = context.getBean(
                            RetryPolicyRegistry.class
                    );
                    assertThat(registry.getRequired("remote").getMaxAttempts())
                            .isEqualTo(4);
                    assertThat(registry.getRequired("remote").getMaxDuration())
                            .hasSeconds(2);
                    assertThat(registry.getRequired("remote").getMaxCauseDepth())
                            .isEqualTo(8);
                });
    }

    @Test
    void shouldUseDedicatedCustomRetryIdGenerator() {
        contextRunner
                .withPropertyValues("iron.retry.publish-spring-events=false")
                .withBean(
                        RetryAutoConfiguration.RETRY_ID_GENERATOR_BEAN_NAME,
                        StringIdGenerator.class,
                        () -> new FixedStringIdGenerator("custom-retry-id")
                )
                .run(context -> {
                    RetryExecutor executor = context.getBean(RetryExecutor.class);
                    RetryPolicy policy = RetryPolicy.builder("single-attempt")
                            .maxAttempts(1)
                            .build();

                    var result = executor.execute(
                            RetryExecution.builder(
                                    "custom-id-operation",
                                    retryContext -> "ok",
                                    policy
                            ).build()
                    );

                    assertThat(result.getStatus()).isEqualTo(RetryStatus.SUCCESS);
                    assertThat(result.getRetryId()).isEqualTo("custom-retry-id");
                });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        contextRunner
                .withPropertyValues("iron.retry.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(RetryExecutor.class));
    }
    @Test
    void shouldResolveRetryGeneratorFromFoundationRegistry() {
        StringIdGeneratorRegistry registry = StringIdGeneratorRegistry.builder()
                .register(
                        RetryAutoConfiguration.RETRY_ID_GENERATOR_REGISTRY_NAME,
                        new FixedStringIdGenerator("registry-retry-id")
                )
                .build();

        contextRunner
                .withPropertyValues("iron.retry.publish-spring-events=false")
                .withBean(StringIdGeneratorRegistry.class, () -> registry)
                .run(context -> {
                    RetryExecutor executor = context.getBean(RetryExecutor.class);
                    RetryPolicy policy = RetryPolicy.builder("registry-id").build();

                    var result = executor.execute(
                            "registry-id-operation",
                            retryContext -> "ok",
                            policy
                    );

                    assertThat(result.getRetryId()).isEqualTo("registry-retry-id");
                });
    }

    @Test
    void shouldFailWhenFoundationRegistryDoesNotContainRetryGenerator() {
        StringIdGeneratorRegistry registry = StringIdGeneratorRegistry.builder()
                .register("message", new FixedStringIdGenerator("message-id"))
                .build();

        contextRunner
                .withPropertyValues("iron.retry.publish-spring-events=false")
                .withBean(StringIdGeneratorRegistry.class, () -> registry)
                .run(context -> assertThat(context).hasFailed());
    }

}
