package com.xjtu.iron.retry.core.time;

import java.time.Duration;

/**
 * 同步重试等待抽象。
 *
 * <p>该接口主要用于隔离 Thread.sleep 并提升核心执行器的可测试性。</p>
 */
@FunctionalInterface
public interface RetrySleeper {

    /**
     * 阻塞当前线程指定时长。
     */
    void sleep(Duration duration) throws InterruptedException;
}
