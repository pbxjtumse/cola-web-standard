package com.xjtu.iron.retry.api;

/** 根据一次完整尝试快照决定是否成功、重试、停止或中止。 */
@FunctionalInterface
public interface RetryClassifier {

    /**
     * 对一次已经完成的物理尝试进行分类。
     *
     * @param attempt 已完成尝试的不可变快照
     * @return 非空重试决策
     */
    RetryDecision classify(RetryAttempt<?> attempt);
}
