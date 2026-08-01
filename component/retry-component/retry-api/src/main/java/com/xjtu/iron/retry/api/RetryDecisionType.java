package com.xjtu.iron.retry.api;

/** 描述分类器对一次已完成尝试给出的下一步动作。 */
public enum RetryDecisionType {
    /** 接受当前返回结果并结束逻辑执行。 */
    SUCCESS,
    /** 当前尝试未达到目标，允许再次执行。 */
    RETRY,
    /** 当前失败或结果不可重试，正常停止。 */
    STOP,
    /** 因取消、安全或协议错误等原因立即终止。 */
    ABORT
}
