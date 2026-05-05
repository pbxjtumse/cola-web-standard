package com.xjtu.iron.cola.web.tracing;

/**
 * Tracing 实现提供者。
 *
 * <p>用于解耦具体 tracing 后端或 SDK 实现。</p>
 *
 * <p>例如：</p>
 * <pre>
 * OTelTraceProvider       -> OpenTelemetry 实现
 * SkyWalkingTraceProvider -> SkyWalking 实现
 * NoopTraceProvider       -> 空实现
 * </pre>
 *
 * <p>starter 模块可以根据配置选择不同 Provider。</p>
 */
public interface TraceProvider {

    /**
     * Provider 名称。
     *
     * <p>例如：</p>
     * <pre>
     * otel
     * skywalking
     * noop
     * </pre>
     */
    String name();


    /**
     * 创建 TraceService。
     *
     * @param instrumentationName 当前服务或当前 SDK 名称
     * @return TraceService 实例
     */
    ITraceService createTraceService(String instrumentationName);
}
