package com.xjtu.iron.relational.spi;

import java.time.Duration;

/**
 * SQL 执行生命周期监听扩展点。
 *
 * <p>后续 Micrometer / Trace / Slow SQL 通过该 SPI 接入；Relational Core 不直接绑定
 * Micrometer 或 OpenTelemetry。</p>
 */
public interface SqlExecutionListener {

    void beforeExecute(SqlExecutionContext context);

    void afterSuccess(SqlExecutionContext context, Duration elapsed);

    void afterFailure(SqlExecutionContext context, Duration elapsed, Throwable failure);
}
