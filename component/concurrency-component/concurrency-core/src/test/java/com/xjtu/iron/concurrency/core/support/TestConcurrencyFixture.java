package com.xjtu.iron.concurrency.core.support;

import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.execution.executor.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncExecutor;
import com.xjtu.iron.concurrency.core.context.DefaultContextAwareTaskDecorator;
import com.xjtu.iron.concurrency.core.context.NoopContextPropagator;
import com.xjtu.iron.concurrency.core.control.DefaultTaskCancellationManager;
import com.xjtu.iron.concurrency.core.control.DefaultTaskControlRegistry;
import com.xjtu.iron.concurrency.core.error.CompositeAsyncErrorClassifier;
import com.xjtu.iron.concurrency.core.error.DefaultAsyncErrorClassifier;
import com.xjtu.iron.concurrency.core.execution.DefaultThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.lifecycle.DefaultTaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.lifecycle.TaskLifecyclePublisher;
import com.xjtu.iron.concurrency.core.listener.CompositeTaskExecutionListener;
import com.xjtu.iron.concurrency.core.listener.NoopAsyncUncaughtExceptionHandler;
import com.xjtu.iron.concurrency.core.metrics.NoopConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.pipeline.DefaultTaskResultPipeline;
import com.xjtu.iron.concurrency.core.registry.DefaultTaskExecutionRegistry;
import com.xjtu.iron.concurrency.core.task.DefaultTaskExecutionTemplate;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 组装真实 core 链路的测试夹具：AsyncExecutor -> DefaultTaskExecutionTemplate -> TaskCommand -> Pipeline。
 */
public final class TestConcurrencyFixture implements AutoCloseable {

    private final String executorName;
    private final ThreadPoolExecutor businessExecutor;
    private final ThreadPoolExecutor fallbackExecutor;
    private final ScheduledThreadPoolExecutor timeoutScheduler;
    private final DefaultThreadPoolRegistry threadPoolRegistry;
    private final DefaultTaskControlRegistry taskControlRegistry;
    private final DefaultTaskCancellationManager cancellationManager;
    private final DefaultTaskExecutionRegistry taskExecutionRegistry;
    private final AsyncErrorClassifier errorClassifier;
    private final TaskLifecyclePublisher lifecyclePublisher;
    private final DefaultTaskExecutionTemplate taskExecutionTemplate;
    private final AsyncExecutor asyncExecutor;

    private TestConcurrencyFixture(
            String executorName,
            ThreadPoolExecutor businessExecutor,
            TaskExecutionListener listener
    ) {
        this.executorName = executorName;
        this.businessExecutor = businessExecutor;
        this.fallbackExecutor = TestExecutors.fixed("fallback", 2);
        this.timeoutScheduler = TestExecutors.scheduler("timeout");
        this.threadPoolRegistry = new DefaultThreadPoolRegistry();
        this.threadPoolRegistry.register(executorName, businessExecutor);
        this.taskControlRegistry = new DefaultTaskControlRegistry();
        this.cancellationManager = new DefaultTaskCancellationManager(taskControlRegistry);
        this.taskExecutionRegistry = new DefaultTaskExecutionRegistry();
        this.errorClassifier = new CompositeAsyncErrorClassifier(List.of(), new DefaultAsyncErrorClassifier());

        TaskExecutionListener compositeListener = listener == null
                ? new CompositeTaskExecutionListener(List.of())
                : new CompositeTaskExecutionListener(List.of(listener));

        this.lifecyclePublisher = new DefaultTaskLifecyclePublisher(
                new NoopConcurrencyMetricsRecorder(),
                taskExecutionRegistry,
                compositeListener
        );

        this.taskExecutionTemplate = new DefaultTaskExecutionTemplate(
                threadPoolRegistry,
                new DefaultContextAwareTaskDecorator(new NoopContextPropagator()),
                lifecyclePublisher,
                new DefaultTaskResultPipeline(
                        errorClassifier,
                        lifecyclePublisher,
                        timeoutScheduler,
                        fallbackExecutor
                ),
                errorClassifier,
                new NoopAsyncUncaughtExceptionHandler(),
                taskControlRegistry,
                cancellationManager
        );

        this.asyncExecutor = new DefaultAsyncExecutor(taskExecutionTemplate, cancellationManager);
    }

    public static TestConcurrencyFixture create() {
        return create(new RecordingTaskExecutionListener());
    }

    public static TestConcurrencyFixture create(TaskExecutionListener listener) {
        return create("default", TestExecutors.fixed("business", 4), listener);
    }

    public static TestConcurrencyFixture create(
            String executorName,
            ThreadPoolExecutor businessExecutor,
            TaskExecutionListener listener
    ) {
        return new TestConcurrencyFixture(executorName, businessExecutor, listener);
    }

    public String executorName() {
        return executorName;
    }

    public AsyncExecutor asyncExecutor() {
        return asyncExecutor;
    }

    public DefaultTaskExecutionTemplate taskExecutionTemplate() {
        return taskExecutionTemplate;
    }

    public TaskExecutionRegistry taskExecutionRegistry() {
        return taskExecutionRegistry;
    }

    public DefaultTaskControlRegistry taskControlRegistry() {
        return taskControlRegistry;
    }

    public ThreadPoolExecutor businessExecutor() {
        return businessExecutor;
    }

    @Override
    public void close() {
        businessExecutor.shutdownNow();
        fallbackExecutor.shutdownNow();
        timeoutScheduler.shutdownNow();
    }
}
