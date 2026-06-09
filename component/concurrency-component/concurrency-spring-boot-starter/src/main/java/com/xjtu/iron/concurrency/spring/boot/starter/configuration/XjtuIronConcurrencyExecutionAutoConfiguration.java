package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.execution.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.AsyncTemplate;
import com.xjtu.iron.concurrency.api.execution.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolManager;
import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.config.ThreadPoolSpecResolver;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncExecutor;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncTemplate;
import com.xjtu.iron.concurrency.core.execution.DefaultRejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.core.registry.DefaultTaskExecutionRegistry;
import com.xjtu.iron.concurrency.core.task.DefaultTaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolFactory;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolManager;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.listener.NoopAsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.spi.RejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolFactory;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;
import com.xjtu.iron.concurrency.spring.boot.starter.resolver.PropertiesThreadPoolSpecResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 并发组件执行链路自动装配。
 */
@AutoConfiguration(after = XjtuIronConcurrencyContextAutoConfiguration.class)
public class XjtuIronConcurrencyExecutionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolSpecResolver threadPoolSpecResolver(XjtuIronConcurrencyProperties properties) {
        return new PropertiesThreadPoolSpecResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory() {
        return new DefaultRejectedExecutionHandlerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolFactory threadPoolFactory(RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory) {
        return new DefaultThreadPoolFactory(rejectedExecutionHandlerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolRegistry threadPoolRegistry(
            ThreadPoolSpecResolver threadPoolSpecResolver,
            ThreadPoolFactory threadPoolFactory,
            XjtuIronConcurrencyProperties properties
    ) {
        Map<String, ThreadPoolSpec> specs = threadPoolSpecResolver.resolveAll();
        if (!specs.containsKey(properties.getDefaultExecutor())) {
            throw new IllegalStateException("Default executor [" + properties.getDefaultExecutor() + "] not found");
        }
        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        specs.forEach((poolName, spec) -> registry.register(poolName, threadPoolFactory.create(spec)));
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolManager threadPoolManager(
            ThreadPoolRegistry threadPoolRegistry,
            RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory
    ) {
        return new DefaultThreadPoolManager(threadPoolRegistry, rejectedExecutionHandlerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutionRegistry taskExecutionRegistry(XjtuIronConcurrencyProperties properties) {
        return new DefaultTaskExecutionRegistry(properties.getTaskRegistry().getMaxSize());
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncTemplate asyncTemplate() {
        return new DefaultAsyncTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler() {
        return new NoopAsyncUncaughtExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutionTemplate taskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator contextAwareTaskDecorator,
            AsyncTemplate asyncTemplate,
            ConcurrencyMetricsRecorder concurrencyMetricsRecorder,
            ObjectProvider<TaskExecutionListener> listeners,
            AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler,
            TaskExecutionRegistry taskExecutionRegistry
    ) {
        List<TaskExecutionListener> listenerList = listeners.orderedStream().toList();
        return new DefaultTaskExecutionTemplate(
                threadPoolRegistry,
                contextAwareTaskDecorator,
                asyncTemplate,
                concurrencyMetricsRecorder,
                listenerList,
                asyncUncaughtExceptionHandler,
                taskExecutionRegistry
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor(TaskExecutionTemplate taskExecutionTemplate) {
        return new DefaultAsyncExecutor(taskExecutionTemplate);
    }

    @Bean
    public DisposableBean threadPoolShutdownHook(ThreadPoolRegistry threadPoolRegistry, ThreadPoolSpecResolver threadPoolSpecResolver) {
        return () -> {
            Map<String, ThreadPoolSpec> specs = threadPoolSpecResolver.resolveAll();
            for (Map.Entry<String, ThreadPoolExecutor> entry : threadPoolRegistry.snapshot().entrySet()) {
                String poolName = entry.getKey();
                ThreadPoolExecutor executor = entry.getValue();
                ThreadPoolSpec spec = specs.get(poolName);
                if (spec == null || !spec.isWaitForTasksToCompleteOnShutdown()) {
                    executor.shutdownNow();
                    continue;
                }
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(spec.getAwaitTermination().toMillis(), TimeUnit.MILLISECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                }
            }
        };
    }
}
