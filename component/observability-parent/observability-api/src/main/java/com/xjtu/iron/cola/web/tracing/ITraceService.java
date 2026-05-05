package com.xjtu.iron.cola.web.tracing;

/**
 * 链路追踪服务入口。
 *
 * <p>业务层、模板层、AOP 层都通过该接口创建 Span、获取 traceId/spanId。</p>
 *
 * <p>该接口是稳定抽象，不绑定任何具体实现。具体实现可以是 OpenTelemetry、SkyWalking 或公司自研 tracing 系统。</p>
 */
public interface ITraceService {

    /**
     * 创建一个新的 Span。
     *
     * <p>spanName 应该具有业务可读性，例如：</p>
     * <pre>
     * order.create
     * order.lockInventory
     * payment.submit
     * </pre>
     */
    ITraceSpan startSpan(String name);

    /**
     * 获取当前链路 traceId。
     *
     * <p>traceId 表示一次完整请求的全局链路 ID。
     * 日志、链路、指标可以通过 traceId 进行关联。</p>
     */
    String traceId();

    /**
     * 获取当前 Span 的 spanId。
     *
     * <p>spanId 表示当前链路节点 ID。
     * 一个 traceId 下通常会有多个 spanId。</p>
     */
    String spanId();
}
