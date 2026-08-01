package com.xjtu.iron.retry.core.time;

import java.time.Duration;

/** 同步重试等待抽象，用于隔离 Thread.sleep 并便于测试。 */
@FunctionalInterface
public interface RetrySleeper {

    void sleep(Duration duration) throws InterruptedException;
}
