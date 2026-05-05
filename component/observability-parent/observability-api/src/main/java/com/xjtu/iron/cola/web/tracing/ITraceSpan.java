package com.xjtu.iron.cola.web.tracing;

/**
 * 1. 把你的 ITraceSpan 接口适配到 OpenTelemetry Span
 * 2. 负责 tag 类型转换
 * 3. 负责异常记录
 * 4. 负责调用 TraceErrorResolver 做异常扩展解析
 * 5. 负责关闭 Scope，防止上下文污染
 * 6. 负责结束 Span
 * 7. 防止重复 close
 * 一个链路追踪节点。
 *
 * <p>一次请求会产生一个 traceId，一个 traceId 下可以有多个 Span。
 * 每个 Span 表示一次方法调用、一次数据库访问、一次远程调用、或一个关键业务代码块。</p>
 * <p>本接口是对具体链路系统 Span 的抽象。业务代码不应该直接依赖 OpenTelemetry、SkyWalking 等具体实现。</p>
 */
public interface ITraceSpan extends AutoCloseable{

    void tag(String key, String value);

    void tag(String key, Number value);

    void tag(String key, Boolean value);

    void tag(String key, Object value);

    /**
     * 记录异常。
     *
     * <p>这个方法只负责把异常记录到当前 Span 上，不负责吞掉异常。</p>
     *
     * <p>正确使用方式：</p>
     * <pre>
     * catch (Throwable e) {
     *     span.error(e);
     *     throw e;
     * }
     * </pre>
     */
    void error(Throwable throwable);

    /**
     * 结束当前 Span。
     *
     * <p>必须调用，否则 Span 耗时不完整，链路可能无法正确上报。</p>
     *
     * <p>推荐配合 try-with-resources 使用：</p>
     * <pre>
     * try (ITraceSpan span = traceService.startSpan("order.create")) {
     *     ...
     * }
     * </pre>
     */
    @Override
    void close();
}
