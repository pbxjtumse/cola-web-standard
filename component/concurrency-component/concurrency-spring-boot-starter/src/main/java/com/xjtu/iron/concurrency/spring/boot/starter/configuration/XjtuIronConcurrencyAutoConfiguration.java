package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(XjtuIronConcurrencyProperties.class)
@ConditionalOnProperty(
        prefix = "xjtu.iron.concurrency",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import({
        XjtuIronConcurrencyContextAutoConfiguration.class,
        XjtuIronConcurrencyExecutionAutoConfiguration.class,
        XjtuIronConcurrencyObservabilityAutoConfiguration.class,
        XjtuIronConcurrencyDiagnosticsAutoConfiguration.class
})
public class XjtuIronConcurrencyAutoConfiguration {
}