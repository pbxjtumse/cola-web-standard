package com.xjtu.iron.cola.web.tracing;

/**
 * 链路追踪服务入口。
 *
 * <p>这是业务代码、TraceTemplate、TraceAspect 创建 Span 和读取 traceId/spanId 的统一入口。</p>
 *
 * <p>它是可观测组件最核心的抽象之一，不绑定 OpenTelemetry、SkyWalking 或任何公司自研 tracing 系统。</p>
 *
 * <p>不同 Provider 会提供不同实现：</p>
 * <ul>
 *     <li>OpenTelemetry：OtelTraceServiceImpl</li>
 *     <li>SkyWalking：SkyWalkingTraceServiceImpl，后续扩展</li>
 *     <li>Noop：NoopTraceServiceImpl</li>
 * </ul>
 *
 * <p>使用原则：</p>
 * <ul>
 *     <li>业务侧不要直接依赖 OpenTelemetry Span</li>
 *     <li>业务侧不要直接依赖 SkyWalking Toolkit</li>
 *     <li>业务侧只通过 ITraceService / ITraceSpan 操作链路</li>
 * </ul>
 */
public interface ITraceService {
    /**
     * 创建一个新的 Span。
     *
     * <p>Span 表示一次链路中的一个节点，例如：</p>
     * <ul>
     *     <li>一次 Controller 方法调用</li>
     *     <li>一次 Application Service 方法调用</li>
     *     <li>一次 TraceTemplate 包裹的业务片段</li>
     *     <li>一次远程调用、数据库调用、缓存调用，通常由 Java Agent 自动创建</li>
     * </ul>
     *
     * <p>spanName 应该具有业务可读性，例如。其实就是核心领域层的方法名称</p>
     * <pre>
     * order.create
     * order.lockInventory
     * payment.submit
     * demo.template.inner
     * </pre>
     *
     * <p>调用方必须最终关闭返回的 ITraceSpan，否则 Span 耗时和上下文可能不完整。</p>
     *
     * @param spanName Span 名称
     * @return 当前创建的 Span；正常实现不应该返回 null，异常情况下可返回 NoopTraceSpan
     */
    ITraceSpan startSpan(String spanName);

    /**
     * 获取当前 traceId。
     *
     * <p>traceId 表示一次完整请求或完整调用链的全局 ID。</p>
     *
     * <p>同一次请求内，请求级 Span、方法级 Span、代码块级 Span 通常共享同一个 traceId。</p>
     *
     * <p>日志、Trace 后端、告警排查通常优先使用 traceId 作为主索引。</p>
     *
     * @return 当前 traceId；如果当前没有有效链路上下文，可以返回空字符串
     */
    String traceId();

    /**
     * 获取当前 spanId。
     *
     * <p>spanId 表示当前链路中的某一个具体节点。</p>
     *
     * <p>同一个 traceId 下通常有多个 spanId，例如：</p>
     * <pre>
     * traceId = T-001
     *   spanId = S-HTTP    GET /template
     *   spanId = S-METHOD  demo.template.api
     *   spanId = S-INNER   demo.template.inner
     * </pre>
     *
     * <p>spanId 是辅助定位字段，日志排查时主索引仍然应该是 traceId。</p>
     *
     * @return 当前 spanId；如果当前没有有效 Span，可以返回空字符串
     */
    String spanId();
}
