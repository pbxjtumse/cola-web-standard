package com.xjtu.iron.concurrency.api.error;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;

/**
 * 异步错误分类器。
 *
 * <p>
 * 并行组件默认只能识别通用技术异常。
 * 业务系统可以实现该接口，把自己的领域异常、应用异常、RPC 异常映射为 AsyncError。
 * </p>
 */
public interface AsyncErrorClassifier {

    /**
     * 对异常进行分类，并生成 AsyncError。
     *
     * @param task 任务模型
     * @param throwable 原始异常
     * @param stage 异常发生阶段
     * @return 异步错误描述
     */
    AsyncError classify(AsyncTask<?> task, Throwable throwable, AsyncErrorStage stage);
}