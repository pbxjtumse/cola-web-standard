package com.xjtu.iron.cache.integrations.observability;


import com.xjtu.iron.cache.core.trace.CacheTraceContext;
import org.slf4j.MDC;

/**
 * 基于 MDC 的 trace 上下文。
 *
 * <p>从日志 MDC 中读取 traceId / spanId。</p>
 *
 * <p>兼容两种常见命名：</p>
 *
 * <pre>
 * trace_id / span_id
 * traceId / spanId
 * </pre>
 */
public class MdcCacheTraceContext implements CacheTraceContext {

    @Override
    public String currentTraceId() {
        String traceId = MDC.get("trace_id");

        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }

        return traceId;
    }

    @Override
    public String currentSpanId() {
        String spanId = MDC.get("span_id");

        if (spanId == null || spanId.isBlank()) {
            spanId = MDC.get("spanId");
        }

        return spanId;
    }
}
