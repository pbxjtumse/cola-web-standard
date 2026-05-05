package com.xjtu.iron.cola.web.tracing;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Trace 日志上下文工具。
 *
 * <p>负责把 traceId / spanId 写入 MDC，
 * 让日志可以通过 %X{traceId} / %X{spanId} 打印链路信息。</p>
 *
 * <p>这里使用 MdcScope 而不是简单 clear()，
 * 是为了支持嵌套 Span。</p>
 */
public final class TraceMdc {

    public static final String TRACE_ID = "traceId";

    public static final String SPAN_ID = "spanId";

    private TraceMdc() {
    }

    /**
     * 写入当前 traceId / spanId，并返回一个作用域对象。
     *
     * <p>调用方必须在 finally 中调用返回对象的 close()，
     * 用于恢复进入前的 MDC。</p>
     */
    public static MdcScope put(ITraceService traceService) {
        Map<String, String> previous = MDC.getCopyOfContextMap();

        if (traceService != null) {
            MDC.put(TRACE_ID, safe(traceService.traceId()));
            MDC.put(SPAN_ID, safe(traceService.spanId()));
        }

        return MdcScope.of(previous);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * MDC 作用域。
     *
     * <p>进入时保存旧 MDC，退出时恢复旧 MDC。</p>
     */
    public static final class MdcScope implements AutoCloseable {

        private final Map<String, String> previous;

        private final boolean noop;

        private MdcScope(Map<String, String> previous, boolean noop) {
            this.previous = previous;
            this.noop = noop;
        }

        public static MdcScope of(Map<String, String> previous) {
            return new MdcScope(previous, false);
        }

        public static MdcScope noop() {
            return new MdcScope(null, true);
        }

        @Override
        public void close() {
            if (noop) {
                return;
            }

            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }
}