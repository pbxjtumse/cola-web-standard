package com.xjtu.iron.cola.web.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;

/**
 * @author pangbo
 */
public class OtelTraceService implements ITraceService {

    private final Tracer tracer;

    public OtelTraceService(String instrumentationName) {
        this.tracer = GlobalOpenTelemetry.getTracer(instrumentationName);
    }

    @Override
    public ITraceSpan startSpan(String spanName) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        return new OtelTraceSpan(span);
    }

    @Override
    public String traceId() {
        SpanContext spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getTraceId() : "";
    }

    @Override
    public String spanId() {
        SpanContext spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getSpanId() : "";
    }
}