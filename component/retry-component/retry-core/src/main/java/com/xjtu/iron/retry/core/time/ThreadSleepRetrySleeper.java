package com.xjtu.iron.retry.core.time;

import java.time.Duration;
import java.util.Objects;

/**
 * 基于 Thread.sleep 的默认同步等待实现。
 */
public final class ThreadSleepRetrySleeper implements RetrySleeper {

    @Override
    public void sleep(Duration duration) throws InterruptedException {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (duration.isZero()) {
            return;
        }
        long millis = duration.toMillis();
        int nanos = duration.minusMillis(millis).getNano();
        Thread.sleep(millis, nanos);
    }
}
