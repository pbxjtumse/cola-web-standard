package com.xjtu.iron.concurrency.core.pipeline;

import com.xjtu.iron.concurrency.core.task.TaskCommand;
import com.xjtu.iron.concurrency.core.task.TaskExecutionContext;

import java.util.concurrent.CompletableFuture;

/**
 * 任务结果处理管道。
 *
 * <p>
 * 在原始任务 Future 之上附加 timeout、fallback 等结果层能力。
 * 原始任务执行仍由 TaskCommand 负责。
 * </p>
 */
public interface TaskResultPipeline {

    /**
     * 构建最终返回给调用方的 Future。
     *
     * @param context 任务执行上下文
     * @param command 原始任务命令
     * @param <T> 任务结果类型
     * @return 应用 timeout、fallback 后的最终 Future
     */
    <T> CompletableFuture<T> apply(
            TaskExecutionContext<T> context,
            TaskCommand<T> command
    );
}
