package com.xjtu.iron.retry.core.time;

import com.xjtu.iron.foundation.time.ClockProvider;

/**
 * 为重试执行器同时提供可测试的墙上时间和单调时间。
 *
 * <p>墙上时间用于事件时间戳，单调时间用于计算执行耗时，避免系统时钟回拨影响重试预算。</p>
 */
public interface RetryClock extends ClockProvider {

    /** 返回只用于计算时间间隔的单调纳秒值。 */
    long nanoTime();
}
