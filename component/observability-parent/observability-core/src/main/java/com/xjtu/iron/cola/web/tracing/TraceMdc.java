package com.xjtu.iron.cola.web.tracing;

import org.slf4j.MDC;

public final class TraceMdc {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    private TraceMdc() {
    }

    public static void put(ITraceService traceService) {
        if (traceService == null) {
            return;
        }

        MDC.put(TRACE_ID, safe(traceService.traceId()));
        MDC.put(SPAN_ID, safe(traceService.spanId()));
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
