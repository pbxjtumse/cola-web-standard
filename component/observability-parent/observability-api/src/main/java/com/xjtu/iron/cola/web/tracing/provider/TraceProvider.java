package com.xjtu.iron.cola.web.tracing.provider;

import com.xjtu.iron.cola.web.enums.TraceProviderType;
import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.context.TraceProviderContext;

/**
 * Trace 实现提供者。
 *
 * <p>TraceProvider 是 Provider 层扩展点，用于根据配置创建具体的 ITraceService 实现。</p>
 *
 * <p>它解决的是“代码层 provider 可切换”的问题，例如：</p>
 * <ul>
 *     <li>provider=otel 使用 OtelTraceServiceImpl</li>
 *     <li>provider=noop 使用 NoopTraceServiceImpl</li>
 *     <li>provider=skywalking 后续使用 SkyWalkingTraceServiceImpl</li>
 * </ul>
 *
 * <p>注意：TraceProvider 不负责启动 Java Agent。</p>
 *
 * <p>Java Agent 是部署层能力，通过 JVM 参数或容器启动参数控制，例如：</p>
 * <pre>
 * -javaagent:/opt/otel/opentelemetry-javaagent.jar
 * -javaagent:/opt/skywalking/skywalking-agent.jar
 * </pre>
 *
 * <p>因此：</p>
 * <ul>
 *     <li>provider 表示代码层使用哪个 ITraceService 实现</li>
 *     <li>agent-type 表示部署层当前使用哪种 Java Agent</li>
 * </ul>
 */
public interface TraceProvider {
    /**
     * Provider 类型。
     *
     * <p>用于和配置项 xy.observability.provider 匹配。</p>
     *
     * <p>例如：</p>
     * <pre>
     * OTEL
     * SKYWALKING
     * NOOP
     * </pre>
     *
     * @return Provider 类型
     */
    TraceProviderType type();


    /**
     * 创建 ITraceService。
     *
     * <p>starter 会根据配置找到匹配的 TraceProvider，然后调用该方法创建真正的 traceService。</p>
     *
     * @param context Provider 创建上下文，包括 instrumentationName、errorResolvers 等
     * @return trace service 实例
     */
    ITraceService createTraceService(TraceProviderContext context);
}
