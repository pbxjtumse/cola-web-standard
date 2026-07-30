package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RetryAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class));

    @Test
    void shouldCreateExecutorAndConfiguredPolicy() {
        contextRunner
                .withPropertyValues(
                        "iron.retry.policies.remote.max-attempts=4",
                        "iron.retry.policies.remote.max-duration=3s",
                        "iron.retry.policies.remote.retry-on[0]=" + IOException.class.getName(),
                        "iron.retry.policies.remote.backoff.type=fixed",
                        "iron.retry.policies.remote.backoff.delay=10ms"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RetryExecutor.class);
                    assertThat(context).hasSingleBean(RetryPolicyRegistry.class);
                    RetryPolicyRegistry registry = context.getBean(RetryPolicyRegistry.class);
                    assertThat(registry.getRequired("remote").getMaxAttempts()).isEqualTo(4);
                });
    }

    @Test
    void shouldBackOffWhenDisabled() {
        contextRunner
                .withPropertyValues("iron.retry.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RetryExecutor.class));
    }
}
