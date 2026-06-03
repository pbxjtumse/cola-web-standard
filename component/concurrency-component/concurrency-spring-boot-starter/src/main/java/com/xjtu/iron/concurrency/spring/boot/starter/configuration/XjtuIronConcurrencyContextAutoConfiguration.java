package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.context.ContextPropagator;
import com.xjtu.iron.concurrency.core.context.CompositeContextPropagator;
import com.xjtu.iron.concurrency.core.context.DefaultContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.core.context.NoopContextPropagator;
import com.xjtu.iron.concurrency.integration.observability.MdcContextPropagator;
import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class XjtuIronConcurrencyContextAutoConfiguration {

    @Bean(name = "ironMdcContextPropagator")
    @ConditionalOnClass(name = "org.slf4j.MDC")
    @ConditionalOnProperty(
            prefix = "xjtu.iron.concurrency.context",
            name = "mdc-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "ironMdcContextPropagator")
    public ContextPropagator mdcContextPropagator() {
        return new MdcContextPropagator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContextAwareTaskDecorator contextAwareTaskDecorator(
            XjtuIronConcurrencyProperties properties,
            ObjectProvider<ContextPropagator> contextPropagators
    ) {
        if (!properties.getContext().isEnabled()) {
            return new DefaultContextAwareTaskDecorator(new NoopContextPropagator());
        }

        List<ContextPropagator> propagatorList = contextPropagators
                .orderedStream()
                .toList();

        if (propagatorList.isEmpty()) {
            return new DefaultContextAwareTaskDecorator(new NoopContextPropagator());
        }

        return new DefaultContextAwareTaskDecorator(
                new CompositeContextPropagator(propagatorList)
        );
    }
}