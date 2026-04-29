package com.xjtu.iron.cola.web.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OTelTraceServiceImpl implements TraceService {
    private final Tracer tracer;

    public OTelTraceServiceImpl(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public TraceSpan startSpan(String name) {
        Span span = tracer.spanBuilder(name).startSpan();
        return new OTelTraceSpanimpl(span);
    }

    @Override
    public void endSpan() {
        Span.current().end();
    }

    @Override
    public String getTraceId() {
        return Span.current().getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId() {
        return Span.current().getSpanContext().getSpanId();
    }
}
