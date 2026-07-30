package com.xjtu.iron.retry.api;

/**
 * 单次尝试完成后的分类决策。
 */
public enum RetryDecision {

    /**
     * 当前结果被认定为成功，结束整个重试过程。
     */
    SUCCESS,

    /**
     * 当前结果或异常允许重试。
     */
    RETRY,

    /**
     * 当前失败不可重试，按不可重试结果结束。
     */
    STOP,

    /**
     * 分类器发现非法状态或必须立即终止的情况。
     */
    ABORT
}
