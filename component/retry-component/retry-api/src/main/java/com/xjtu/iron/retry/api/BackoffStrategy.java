package com.xjtu.iron.retry.api;

/** 计算下一次物理尝试开始前应该等待多久。 */
@FunctionalInterface
public interface BackoffStrategy {

    /**
     * 根据刚刚完成的尝试和分类决策计算退避结果。
     *
     * @param attempt 刚刚完成的尝试
     * @param decision 分类器给出的重试决策
     * @return 非空、非负的等待结果
     */
    RetryDelay nextDelay(RetryAttempt<?> attempt, RetryDecision decision);
}
