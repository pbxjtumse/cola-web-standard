package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.AsyncTask;
import com.xjtu.iron.concurrency.core.spi.TaskExecutionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 默认异步执行器。
 *
 * <p>该类只作为业务入口，实际投递流程交给 {@link TaskExecutionTemplate}。</p>
 */
public class DefaultAsyncExecutor implements AsyncExecutor {

    private final TaskExecutionTemplate taskExecutionTemplate;

    public DefaultAsyncExecutor(TaskExecutionTemplate taskExecutionTemplate) {
        this.taskExecutionTemplate = taskExecutionTemplate;
    }

    @Override
    public void execute(String executorName, String taskName, Runnable runnable) {
        taskExecutionTemplate.execute(executorName, taskName, runnable);
    }

    @Override
    public boolean tryExecute(String executorName, String taskName, Runnable runnable) {
        return taskExecutionTemplate.tryExecute(executorName, taskName, runnable);
    }

    @Override
    public <T> CompletableFuture<T> supply(String executorName, String taskName, Supplier<T> supplier) {
        return taskExecutionTemplate.supply(executorName, taskName, supplier);
    }

    @Override
    public CompletableFuture<Void> run(String executorName, String taskName, Runnable runnable) {
        return taskExecutionTemplate.run(executorName, taskName, runnable);
    }

    @Override
    public <T> CompletableFuture<T> submit(AsyncTask<T> task) {
        return taskExecutionTemplate.submit(task);
    }
}
