package com.xjtu.iron.cola.web.enums;

/**
 * Trace Provider 类型。
 *
 * <p>用于表示当前使用哪一种 trace 实现。</p>
 *
 * <p>注意：ProviderType 是代码层实现选择，不等同于 Java Agent 类型。</p>
 *
 * <p>例如：</p>
 * <ul>
 *     <li>OTEL：使用 OpenTelemetry 实现</li>
 *     <li>SKYWALKING：使用 SkyWalking 实现，后续扩展</li>
 *     <li>NOOP：空实现，不创建真实 Span</li>
 * </ul>
 */
public enum TraceProviderType {
    /**
     * OpenTelemetry Provider。
     */
    OTEL,
    /**
     * SkyWalking Provider。
     *
     * <p>当前可以先预留，后续新增 observability-skywalking 模块实现。</p>
     */
    SKYWALKING,
    /**
     * 空 Provider。
     *
     * <p>用于关闭 tracing、降级、本地调试或无法接入 tracing 的场景。</p>
     */
    NOOP
}
