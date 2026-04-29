package com.xjtu.iron.cola.web.tracing;

public interface TraceService {

    TraceSpan startSpan(String name);

    void endSpan();

    String getTraceId();

    String getSpanId();
}
