package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.execution.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;
import com.xjtu.iron.concurrency.config.ThreadPoolSpecResolver;
import com.xjtu.iron.concurrency.core.execution.DefaultAsyncExecutor;
import com.xjtu.iron.concurrency.core.execution.DefaultAsyncTemplate;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolFactory;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.execution.ThreadPoolFactory;
import com.xjtu.iron.concurrency.core.execution.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;
import com.xjtu.iron.concurrency.spring.boot.starter.resolver.PropertiesThreadPoolSpecResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@AutoConfiguration(after = XjtuIronConcurrencyContextAutoConfiguration.class)
public class XjtuIronConcurrencyExecutionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolSpecResolver threadPoolSpecResolver(
            XjtuIronConcurrencyProperties properties
    ) {
        return new PropertiesThreadPoolSpecResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolFactory threadPoolFactory() {
        return new DefaultThreadPoolFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolRegistry threadPoolRegistry(
            ThreadPoolSpecResolver threadPoolSpecResolver,
            ThreadPoolFactory threadPoolFactory,
            XjtuIronConcurrencyProperties properties
    ) {
        Map<String, ThreadPoolSpec> specs = threadPoolSpecResolver.resolveAll();

        if (specs.isEmpty()) {
            throw new IllegalStateException("No thread pool configured for xjtu.iron.concurrency.thread-pools");
        }

        if (!specs.containsKey(properties.getDefaultExecutor())) {
            throw new IllegalStateException(
                    "Default executor [" + properties.getDefaultExecutor() + "] not found in xjtu.iron.concurrency.thread-pools"
            );
        }

        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();

        specs.forEach((poolName, spec) -> {
            ExecutorService executorService = threadPoolFactory.create(spec);
            registry.register(poolName, executorService);
        });

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncTemplate asyncTemplate() {
        return new DefaultAsyncTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator contextAwareTaskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder concurrencyMetricsRecorder
    ) {
        return new DefaultAsyncExecutor(
                threadPoolRegistry,
                contextAwareTaskDecorator,
                asyncTemplate,
                concurrencyMetricsRecorder
        );
    }

    @Bean
    public DisposableBean threadPoolShutdownHook(
            ThreadPoolRegistry threadPoolRegistry,
            ThreadPoolSpecResolver threadPoolSpecResolver
    ) {
        return () -> {
            Map<String, ThreadPoolSpec> specs = threadPoolSpecResolver.resolveAll();

            for (Map.Entry<String, ExecutorService> entry : threadPoolRegistry.snapshot().entrySet()) {
                String poolName = entry.getKey();
                ExecutorService executorService = entry.getValue();
                ThreadPoolSpec spec = specs.get(poolName);

                if (spec == null) {
                    executorService.shutdownNow();
                    continue;
                }

                if (!spec.isWaitForTasksToCompleteOnShutdown()) {
                    executorService.shutdownNow();
                    continue;
                }

                executorService.shutdown();

                try {
                    boolean terminated = executorService.awaitTermination(
                            spec.getAwaitTermination().toMillis(),
                            TimeUnit.MILLISECONDS
                    );

                    if (!terminated) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    executorService.shutdownNow();
                }
            }
        };
    }
}