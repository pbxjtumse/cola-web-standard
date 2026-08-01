package com.xjtu.iron.retry.config;

import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryIdGenerator;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;
import com.xjtu.iron.retry.core.time.RetryClock;
import com.xjtu.iron.retry.core.time.RetrySleeper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证核心 Spring Boot 自动配置和策略继承。 */
class RetryAutoConfigurationTest {

    /** 创建只加载重试自动配置的轻量上下文。 */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class));

    /** 验证执行器、可替换基础设施和继承策略都会装配。 */
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
                    assertThat(context).hasSingleBean(RetrySleeper.class);
                    assertThat(context).hasSingleBean(RetryIdGenerator.class);
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

    /** 验证总开关关闭时不会装配执行器。 */
    @Test
    void shouldBackOffWhenDisabled() {
        contextRunner
                .withPropertyValues("iron.retry.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(RetryExecutor.class));
    }
}
