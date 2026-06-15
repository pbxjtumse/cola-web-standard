package com.xjtu.iron.concurrency.api.task;

/**
 * 异步任务结果模式。
 */
public enum TaskResultMode {

    /**
     * 只提交任务，不向调用方返回结果 Future。
     */
    FIRE_AND_FORGET,

    /**
     * 调用方需要通过 CompletableFuture 感知任务结果。
     */
    RESULT_AWARE
}
