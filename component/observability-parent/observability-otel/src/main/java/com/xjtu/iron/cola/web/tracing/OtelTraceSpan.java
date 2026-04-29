package com.xjtu.iron.cola.web.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;

public class OtelTraceSpan implements ITraceSpan {

    private final Span span;

    public OtelTraceSpan(Span span) {
        this.span = span;
    }

    @Override
    public void tag(String key, String value) {
        if (key != null && value != null) {
            span.setAttribute(key, value);
        }
    }

    @Override
    public void tag(String key, long value) {
        if (key != null) {
            span.setAttribute(key, value);
        }
    }

    @Override
    public void tag(String key, double value) {
        if (key != null) {
            span.setAttribute(key, value);
        }
    }

    @Override
    public void tag(String key, boolean value) {
        if (key != null) {
            span.setAttribute(key, value);
        }
    }

    @Override
    public void error(Throwable throwable) {
        if (throwable != null) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        }
    }

    @Override
    public void close() {
        span.end();
    }
}
