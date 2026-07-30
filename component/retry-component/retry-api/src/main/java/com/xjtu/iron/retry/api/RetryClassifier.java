package com.xjtu.iron.retry.api;

/**
 * 对单次尝试的返回值或异常进行分类。
 */
@FunctionalInterface
public interface RetryClassifier {

    /**
     * 分类当前尝试结果。
     *
     * <p>同一次调用中，正常返回时 failure 为 null；抛出异常时 result 通常为 null。</p>
     *
     * @param context 当前尝试完成后的上下文
     * @param result 当前尝试返回值
     * @param failure 当前尝试异常
     * @return 重试决策，不能返回 null
     */
    RetryDecision classify(RetryContext context, Object result, Throwable failure);
}
