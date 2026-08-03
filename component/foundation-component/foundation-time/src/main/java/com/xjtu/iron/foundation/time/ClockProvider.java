package com.xjtu.iron.foundation.time;

import java.time.Clock;

/**
 * Clock 提供者。
 *
 * <p>Java 标准库已经提供 Clock，因此 Foundation 不再创造 TimeProvider；这里仅在需要注入、
 * 延迟获取或动态替换 Clock 的场景提供一个极薄抽象。</p>
 */
@FunctionalInterface
public interface ClockProvider {

    Clock clock();
}
