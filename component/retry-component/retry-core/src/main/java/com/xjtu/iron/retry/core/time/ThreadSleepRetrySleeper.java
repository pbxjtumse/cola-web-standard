package com.xjtu.iron.retry.core.time;

import com.xjtu.iron.foundation.core.validation.Arguments;

import java.time.Duration;

/** 使用当前线程休眠完成同步退避等待。 */
public final class ThreadSleepRetrySleeper implements RetrySleeper {

    /** 将 Duration 转换为 Java 17 支持的毫秒和纳秒参数后休眠。 */
    @Override
    public void sleep(Duration duration) throws InterruptedException {
        Duration actualDuration = Arguments.nonNegative(duration, "duration");
        if (actualDuration.isZero()) {
            return;
        }
        long millis = actualDuration.toMillis();
        int nanos = actualDuration.getNano() % 1_000_000;
        Thread.sleep(millis, nanos);
    }
}
