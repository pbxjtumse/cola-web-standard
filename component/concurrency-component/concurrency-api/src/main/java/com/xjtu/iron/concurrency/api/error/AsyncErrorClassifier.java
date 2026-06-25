package com.xjtu.iron.concurrency.api.error;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;

/**
 * 异步错误分类器。
 *
 * <p>
 * 分类器现在只依赖 {@link AsyncErrorClassificationContext}，不再依赖可变的 AsyncTask。
 * 任务提交后，core 会先生成不可变任务快照，并把其中的 TaskMetadata 传入分类上下文。
 * </p>
 */
public interface AsyncErrorClassifier {

    /**
     * 对异常进行分类，并生成 AsyncError。
     *
     * @param context 错误分类上下文
     * @return 异步错误描述
     */
    AsyncError classify(AsyncErrorClassificationContext context);

    /**
     * 便捷方法：从任务元数据、异常和阶段构造上下文后进行分类。
     *
     * @param task 任务元数据快照
     * @param throwable 原始异常
     * @param stage 异常发生阶段
     * @return 异步错误描述
     */
    default AsyncError classify(TaskMetadata task, Throwable throwable, AsyncErrorStage stage) {
        return classify(AsyncErrorClassificationContext.of(task, throwable, stage));
    }
}
