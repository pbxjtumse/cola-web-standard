package com.xjtu.iron.cola.web.tracing;

import com.xjtu.iron.cola.web.tracing.TraceSpan;
import io.opentelemetry.api.trace.Span;

public class OTelTraceSpanimpl implements TraceSpan {
    private final Span span;

    public OTelTraceSpanimpl(Span span) {
        this.span = span;
    }

    @Override
    public void setTag(String key, String value) {
        span.setAttribute(key, value);
    }

    @Override
    public void recordException(Throwable e) {
        span.recordException(e);
    }

    @Override
    public void end() {
        span.end();
    }
}
