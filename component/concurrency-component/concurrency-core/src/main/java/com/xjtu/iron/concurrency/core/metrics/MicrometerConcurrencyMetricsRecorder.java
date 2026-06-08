package com.xjtu.iron.concurrency.core.metrics;

/**
 * @deprecated Micrometer 实现放在 spring-boot-starter/observability 中，避免 core 直接依赖 Micrometer。
 */
@Deprecated
public class MicrometerConcurrencyMetricsRecorder extends NoopConcurrencyMetricsRecorder {
}
