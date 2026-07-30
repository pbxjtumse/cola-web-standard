package com.xjtu.iron.retry.api;

import java.time.Duration;

/**
 * 下一次尝试前的退避时间计算策略。
 */
@FunctionalInterface
public interface BackoffStrategy {

    /**
     * 根据刚刚完成的尝试上下文计算下一次等待时间。
     *
     * @param context 当前失败尝试的上下文
     * @return 非负等待时间
     */
    Duration nextDelay(RetryContext context);
}
