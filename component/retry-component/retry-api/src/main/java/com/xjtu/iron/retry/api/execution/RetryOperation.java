package com.xjtu.iron.retry.api.execution;

/** 表示一次可以由重试执行器调用的业务操作。 */
@FunctionalInterface
public interface RetryOperation<T> {

    /**
     * 执行当前物理尝试。
     *
     * @param context 当前尝试上下文
     * @return 当前业务结果
     * @throws Exception 业务操作允许抛出的受检或运行时异常
     */
    T execute(RetryContext context) throws Exception;
}
