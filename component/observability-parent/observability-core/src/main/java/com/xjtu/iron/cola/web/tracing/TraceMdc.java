package com.xjtu.iron.cola.web.tracing;

import org.slf4j.MDC;

public class TraceMdc {

    public static void inject(TraceService traceService) {
        MDC.put("traceId", traceService.getTraceId());
        MDC.put("spanId", traceService.getSpanId());
    }

    public static void clear() {
        MDC.clear();
    }
}
