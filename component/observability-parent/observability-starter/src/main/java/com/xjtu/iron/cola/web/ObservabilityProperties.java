package com.xjtu.iron.cola.web;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xy.observability")
@Data
public class ObservabilityProperties {

    /**
     * Trace Provider。
     *
     * <p>用于决定 ITraceService 使用哪个实现。</p>
     *
     * <p>可选值：</p>
     * <pre>
     * otel
     * skywalking
     * noop
     * </pre>
     */
    private String provider = "otel";

    /**
     * 当前部署使用的 Agent 类型。
     *
     * <p>这个字段主要用于部署说明和配置校验。</p>
     *
     * <p>可选值：</p>
     * <pre>
     * otel
     * skywalking
     * none
     * </pre>
     */
    private String agentType = "otel";

    /**
     * 观测总开关
     */
    private boolean enabled = true;

    /**
     * 当前业务服务名。
     *
     * <p>例如 order-service、payment-service、observability-demo-app。</p>
     *
     * <p>如果不配置，starter 可以从 spring.application.name 兜底获取。</p>
     */
    private String serviceName;


    /**
     * 当前自定义埋点 SDK 的 instrumentation scope 名称。
     *
     * <p>OpenTelemetry Provider 会使用该字段创建 Tracer：</p>
     *
     * <pre>
     * GlobalOpenTelemetry.getTracer(instrumentationScopeName)
     * </pre>
     *
     * <p>它表示“这条自定义 Span 是由哪套 SDK 创建的”，不是服务名。</p>
     */
    private String instrumentationScopeName = "xy-observability";

    /**
     * 是否启用请求级 MDC。
     * 请求级 Span：由 Java Agent 创建，表示整个 HTTP 请求
     * <p>启用后，会在 HTTP 请求进入时，从当前 Trace 上下文读取 traceId/spanId，写入 MDC，并在请求结束后恢复原 MDC。</p>
     *
     * <p>注意：该开关不负责创建请求级 Span。请求级 HTTP Span 推荐由 OpenTelemetry Java Agent 自动创建。</p>
     */
    private boolean webMdcEnabled = true;

    /**
     * 是否创建方法级 Span
     *
     * <p>启用后，标记了 @Trace 的方法会被 AOP 拦截，并创建方法级 Span。</p>
     *
     * <p>这个开关控制的是“方法级 Span 是否创建”。</p>
     */
    private boolean methodTracingEnabled = true;

    /**
     * 是否启用方法级 MDC。
     *
     * <p>当 TraceAspect 创建方法级 Span 后，是否将当前方法 Span 的 traceId/spanId 临时写入 MDC。</p>
     *
     * <p>如果 methodTracingEnabled=false，则不会创建方法级 Span，本配置基本没有意义。</p>
     */
    private boolean methodMdcEnabled = true;

    /**
     * 是否启用模板级 MDC。
     *
     * <p>当 TraceTemplate 创建代码块级 Span 后，是否将当前代码块 Span 的 traceId/spanId 临时写入 MDC。</p>
     */
    private boolean templateMdcEnabled = true;
}
