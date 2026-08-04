package com.xjtu.iron.foundation.time;

import java.time.Clock;
import java.time.Instant;

/**
 * Clock 提供者。
 *
 * <p>Java 标准库已经提供 Clock，因此 Foundation 不再创造 TimeProvider；这里仅在需要注入、
 * 延迟获取或动态替换 Clock 的场景提供一个极薄抽象。</p>
 */
@FunctionalInterface
public interface ClockProvider {

    Clock clock();

    /**
     * 返回当前时间点。
     *
     * <p>
     * 这是 {@code clock().instant()} 的便捷方法，避免调用方每次都写两层调用。
     * </p>
     *
     * @return 当前 Instant
     */
    default Instant now() {
        return clock().instant();
    }

    /**
     * 返回当前毫秒时间戳。
     *
     * <p>
     * 这是 {@code clock().millis()} 的便捷方法。
     * </p>
     *
     * @return 当前毫秒时间戳
     */
    default long currentTimeMillis() {
        return clock().millis();
    }
}
