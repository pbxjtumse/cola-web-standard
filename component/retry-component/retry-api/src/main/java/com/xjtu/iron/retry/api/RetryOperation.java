package com.xjtu.iron.retry.api;

/**
 * 单次可重试业务操作。
 *
 * <p>每次尝试都会重新调用该接口。调用方应确保操作满足天然幂等、业务幂等保护，
 * 或者能够接受重复执行带来的副作用。</p>
 *
 * @param <T> 业务返回值类型
 */
@FunctionalInterface
public interface RetryOperation<T> {

    /**
     * 执行一次业务尝试。
     *
     * @param context 当前尝试的重试上下文
     * @return 当前尝试返回值
     * @throws Exception 当前尝试失败时抛出的异常
     */
    T execute(RetryContext context) throws Exception;
}
