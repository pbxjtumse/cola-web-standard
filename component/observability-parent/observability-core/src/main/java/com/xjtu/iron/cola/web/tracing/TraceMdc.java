package com.xjtu.iron.cola.web.tracing;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Trace 日志上下文工具。
 *
 * <p>负责把当前 trace_id / span_id 写入 MDC，
 * 让日志可以通过 logback pattern 输出链路信息。</p>
 *
 * <p>本类只维护标准字段：</p>
 * <pre>
 * trace_id
 * span_id
 * </pre>
 *
 * <p>不再维护 traceId / spanId，避免日志字段风格混乱。</p>
 */
public final class TraceMdc {

    /**
     * 标准 trace id key。
     */
    public static final String TRACE_ID = "trace_id";

    /**
     * 标准 span id key。
     */
    public static final String SPAN_ID = "span_id";

    private TraceMdc() {
    }

    /**
     * 将当前 Trace 上下文写入 MDC。
     *
     * <p>进入时保存旧 MDC，退出时通过 MdcScope 恢复旧 MDC。</p>
     *
     * <p>这里不能简单 MDC.clear()，因为存在嵌套场景：</p>
     * <pre>
     * 请求级 MDC
     *   └── 方法级 MDC
     *         └── 模板级 MDC
     * </pre>
     *
     * @param traceService trace 服务
     * @return MDC 作用域
     */
    public static MdcScope put(ITraceService traceService) {
        Map<String, String> previous = MDC.getCopyOfContextMap();

        if (traceService != null) {
            String traceId = safe(traceService.traceId());
            String spanId = safe(traceService.spanId());

            putIfNotBlank(TRACE_ID, traceId);
            putIfNotBlank(SPAN_ID, spanId);
        }

        return MdcScope.of(previous);
    }

    /**
     * 获取当前 MDC 中的 trace_id。
     *
     * <p>用于异常响应、响应头、统一日志等场景。</p>
     *
     * @return 当前 trace_id；没有则返回空字符串
     */
    public static String currentTraceId() {
        return safe(MDC.get(TRACE_ID));
    }

    /**
     * 获取当前 MDC 中的 span_id。
     *
     * @return 当前 span_id；没有则返回空字符串
     */
    public static String currentSpanId() {
        return safe(MDC.get(SPAN_ID));
    }

    private static void putIfNotBlank(String key, String value) {
        if (!isBlank(value)) {
            MDC.put(key, value);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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