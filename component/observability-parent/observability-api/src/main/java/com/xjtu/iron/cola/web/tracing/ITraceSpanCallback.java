package com.xjtu.iron.cola.web.tracing;

/**
 * 可操作 Span 的 tracing 回调。
 *
 * <p>主要给 TraceTemplate 使用。与ITraceCallback区别是：该回调会把当前Span暴露给调用方。允许业务代码添加标签。</p>
 *
 * <p>典型使用：</p>
 * <pre>
 * traceTemplate.execute("order.create", span -> {
 *     span.tag("order.channel", cmd.getChannel());
 *     span.tag("order.amount.cent", cmd.getAmountCent());
 *     return orderService.create(cmd);
 * });
 * </pre>
 */
@FunctionalInterface
public interface ITraceSpanCallback<T, E extends Throwable> {
    /**
     * 执行业务逻辑，并允许调用方操作当前 Span。
     *
     * @param span 当前 Span
     * @return 业务返回值
     * @throws E 业务异常或系统异常
     */
    T execute(ITraceSpan span) throws E;
}