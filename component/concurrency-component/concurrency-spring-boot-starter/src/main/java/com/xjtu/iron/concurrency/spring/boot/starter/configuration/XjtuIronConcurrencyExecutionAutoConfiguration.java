package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.api.context.ContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationRule;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.execution.executor.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolManager;
import com.xjtu.iron.concurrency.api.execution.pool.ThreadPoolSpec;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.template.AsyncTemplate;
import com.xjtu.iron.concurrency.api.listener.AsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.config.ThreadPoolSpecResolver;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncExecutor;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncTemplate;
import com.xjtu.iron.concurrency.core.error.CompositeAsyncErrorClassifier;
import com.xjtu.iron.concurrency.core.error.DefaultAsyncErrorClassifier;
import com.xjtu.iron.concurrency.core.execution.*;
import com.xjtu.iron.concurrency.core.lifecycle.DefaultTaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.lifecycle.TaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.listener.CompositeTaskExecutionListener;
import com.xjtu.iron.concurrency.core.listener.NoopAsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.core.listener.NoopTaskExecutionListener;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.pipeline.DefaultTaskResultPipeline;
import com.xjtu.iron.concurrency.core.pipeline.TaskResultPipeline;
import com.xjtu.iron.concurrency.core.registry.DefaultTaskExecutionRegistry;
import com.xjtu.iron.concurrency.core.spi.RejectedExecutionHandlerFactory;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolFactory;
import com.xjtu.iron.concurrency.core.spi.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.task.DefaultTaskExecutionTemplate;
import com.xjtu.iron.concurrency.spring.boot.starter.properties.XjtuIronConcurrencyProperties;
import com.xjtu.iron.concurrency.spring.boot.starter.resolver.PropertiesThreadPoolSpecResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 并发组件执行链路自动装配。
 *
 * <p>
 * 负责创建线程池注册表、错误分类规则链、生命周期发布器、结果处理管道、
 * 任务投递模板和业务入口 AsyncExecutor。
 * </p>
 */
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
    public RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory() {
        return new DefaultRejectedExecutionHandlerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolFactory threadPoolFactory(
            RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory
    ) {
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
            throw new IllegalStateException(
                    "Default executor [" + properties.getDefaultExecutor() + "] not found"
            );
        }

        DefaultThreadPoolRegistry registry = new DefaultThreadPoolRegistry();
        specs.forEach((poolName, spec) ->
                registry.register(poolName, threadPoolFactory.create(spec))
        );
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadPoolManager threadPoolManager(
            ThreadPoolRegistry threadPoolRegistry,
            RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory
    ) {
        return new DefaultThreadPoolManager(
                threadPoolRegistry,
                rejectedExecutionHandlerFactory
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskExecutionRegistry taskExecutionRegistry(
            XjtuIronConcurrencyProperties properties
    ) {
        return new DefaultTaskExecutionRegistry(
                properties.getTaskRegistry().getMaxSize()
        );
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

    /**
     * 创建只处理 JDK 与并行组件通用异常的默认分类器。
     *
     * <p>
     * 该 Bean 作为 CompositeAsyncErrorClassifier 的最后兜底，不直接替代业务规则。
     * </p>
     */
    @Bean(name = "ironDefaultAsyncErrorClassifier")
    @ConditionalOnMissingBean(name = "ironDefaultAsyncErrorClassifier")
    public AsyncErrorClassifier ironDefaultAsyncErrorClassifier() {
        return new DefaultAsyncErrorClassifier();
    }

    /**
     * 创建最终注入任务主链路的组合错误分类器。
     *
     * <p>
     * 业务方可以注册任意数量的 AsyncErrorClassificationRule Bean；
     * Composite 会按 order 从小到大匹配，均不匹配时交给默认分类器。
     * </p>
     */
    @Bean(name = "ironAsyncErrorClassifier")
    @Primary
    @ConditionalOnMissingBean(name = "ironAsyncErrorClassifier")
    public AsyncErrorClassifier ironAsyncErrorClassifier(
            ObjectProvider<AsyncErrorClassificationRule> ruleProvider,
            @Qualifier("ironDefaultAsyncErrorClassifier")
            AsyncErrorClassifier fallbackClassifier
    ) {
        List<AsyncErrorClassificationRule> rules =
                ruleProvider.orderedStream().toList();

        return new CompositeAsyncErrorClassifier(
                rules,
                fallbackClassifier
        );
    }

    /**
     * 将业务注册的多个任务监听器组合成主链路只需依赖的单个监听器。
     */
    @Bean(name = "ironTaskExecutionListener")
    @ConditionalOnMissingBean(name = "ironTaskExecutionListener")
    public TaskExecutionListener ironTaskExecutionListener(
            ObjectProvider<TaskExecutionListener> listeners
    ) {
        List<TaskExecutionListener> listenerList = listeners.orderedStream()
                .filter(listener -> !(listener instanceof CompositeTaskExecutionListener))
                .toList();

        if (listenerList.isEmpty()) {
            return new NoopTaskExecutionListener();
        }

        return new CompositeTaskExecutionListener(listenerList);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskLifecyclePublisher taskLifecyclePublisher(
            ConcurrencyMetricsRecorder concurrencyMetricsRecorder,
            TaskExecutionRegistry taskExecutionRegistry,
            @Qualifier("ironTaskExecutionListener")
            TaskExecutionListener taskExecutionListener
    ) {
        return new DefaultTaskLifecyclePublisher(
                concurrencyMetricsRecorder,
                taskExecutionRegistry,
                taskExecutionListener
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskResultPipeline taskResultPipeline(
            @Qualifier("ironAsyncErrorClassifier")
            AsyncErrorClassifier asyncErrorClassifier,
            TaskLifecyclePublisher taskLifecyclePublisher,
            @Qualifier("ironConcurrencyTimeoutScheduler")
            ScheduledExecutorService timeoutScheduler,
            @Qualifier("ironConcurrencyFallbackExecutor")
            Executor fallbackExecutor
    ) {
        return new DefaultTaskResultPipeline(
                asyncErrorClassifier,
                taskLifecyclePublisher,
                timeoutScheduler,
                fallbackExecutor
        );
    }




    @Bean(
            name = "ironConcurrencyTimeoutScheduler",
            destroyMethod = "shutdownNow"
    )
    @ConditionalOnMissingBean(name = "ironConcurrencyTimeoutScheduler")
    public ScheduledExecutorService ironConcurrencyTimeoutScheduler() {
        ScheduledThreadPoolExecutor scheduler =
                new ScheduledThreadPoolExecutor(
                        1,
                        new NamedThreadFactory("iron-concurrency-timeout-", true),
                        new ThreadPoolExecutor.AbortPolicy()
                );

        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);

        return scheduler;
    }

    @Bean(name = "ironConcurrencyFallbackExecutor", destroyMethod = "shutdownNow")
    @ConditionalOnMissingBean(name = "ironConcurrencyFallbackExecutor")
    public Executor ironConcurrencyFallbackExecutor() {
        int maximumPoolSize = Math.max(
                2,
                Math.min(
                        8,
                        Runtime.getRuntime().availableProcessors()
                )
        );

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                maximumPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024),
                new NamedThreadFactory(
                        "iron-concurrency-fallback-",
                        true
                ),
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.allowCoreThreadTimeOut(true);
        return executor;
    }



    @Bean
    @ConditionalOnMissingBean
    public TaskExecutionTemplate taskExecutionTemplate(
            ThreadPoolRegistry threadPoolRegistry,
            ContextAwareTaskDecorator contextAwareTaskDecorator,
            TaskLifecyclePublisher taskLifecyclePublisher,
            TaskResultPipeline taskResultPipeline,
            @Qualifier("ironAsyncErrorClassifier")
            AsyncErrorClassifier asyncErrorClassifier,
            AsyncUncaughtExceptionHandler asyncUncaughtExceptionHandler
    ) {
        return new DefaultTaskExecutionTemplate(
                threadPoolRegistry,
                contextAwareTaskDecorator,
                taskLifecyclePublisher,
                taskResultPipeline,
                asyncErrorClassifier,
                asyncUncaughtExceptionHandler
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor(
            TaskExecutionTemplate taskExecutionTemplate
    ) {
        return new DefaultAsyncExecutor(taskExecutionTemplate);
    }

    /**
     * 应用关闭时按照每个线程池配置执行优雅停机。
     */
    @Bean
    public DisposableBean threadPoolShutdownHook(
            ThreadPoolRegistry threadPoolRegistry,
            ThreadPoolSpecResolver threadPoolSpecResolver
    ) {
        return () -> {
            Map<String, ThreadPoolSpec> specs = threadPoolSpecResolver.resolveAll();

            for (Map.Entry<String, ThreadPoolExecutor> entry
                    : threadPoolRegistry.snapshot().entrySet()) {
                String poolName = entry.getKey();
                ThreadPoolExecutor executor = entry.getValue();
                ThreadPoolSpec spec = specs.get(poolName);

                if (spec == null || !spec.isWaitForTasksToCompleteOnShutdown()) {
                    executor.shutdownNow();
                    continue;
                }

                executor.shutdown();
                try {
                    if (!executor.awaitTermination(
                            spec.getAwaitTermination().toMillis(),
                            TimeUnit.MILLISECONDS
                    )) {
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
