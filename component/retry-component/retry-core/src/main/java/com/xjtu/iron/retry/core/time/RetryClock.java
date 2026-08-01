package com.xjtu.iron.retry.core.time;

import java.time.Instant;

/** 为执行器同时提供墙上时间和单调时间。 */
public interface RetryClock {

    Instant now();

    long nanoTime();
}
