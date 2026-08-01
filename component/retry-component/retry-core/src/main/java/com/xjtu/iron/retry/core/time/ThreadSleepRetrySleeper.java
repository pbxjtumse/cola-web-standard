package com.xjtu.iron.retry.core.time;

import java.time.Duration;
import java.util.Objects;

/** 使用当前线程休眠完成同步退避等待。 */
public final class ThreadSleepRetrySleeper implements RetrySleeper {

    /** 校验等待时长后调用 Java 21 的 Duration 休眠重载。 */
    @Override
    public void sleep(Duration duration) throws InterruptedException {
        Duration actualDuration = Objects.requireNonNull(
                duration,
                "duration must not be null"
        );
        if (actualDuration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (actualDuration.isZero()) {
            return;
        }
        long millis = actualDuration.toMillis();
        int nanos = actualDuration.getNano() % 1_000_000;
        Thread.sleep(millis,nanos);
    }
}
