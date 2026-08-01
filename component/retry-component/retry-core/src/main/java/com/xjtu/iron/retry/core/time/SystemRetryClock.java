package com.xjtu.iron.retry.core.time;

import java.time.Instant;

/** 基于系统时钟的默认实现。 */
public final class SystemRetryClock implements RetryClock {

    @Override
    public Instant now() {
        return Instant.now();
    }

    @Override
    public long nanoTime() {
        return System.nanoTime();
    }
}
