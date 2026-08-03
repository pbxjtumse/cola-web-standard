package com.xjtu.iron.foundation.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 提供可替换的系统时钟。
 *
 * <p>生产环境通常使用系统时钟，测试环境可以使用可推进时钟，避免依赖真实等待。</p>
 */
public interface ClockProvider {

    /** 返回当前使用的 JDK 时钟。 */
    Clock clock();

    /** 返回当前绝对时间点。 */
    default Instant now() {
        return clock().instant();
    }

    /** 返回当前时区。 */
    default ZoneId zoneId() {
        return clock().getZone();
    }
}
