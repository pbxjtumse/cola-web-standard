package com.xjtu.iron.cola.web.tracing;

public interface TraceSpan {
    void setTag(String key, String value);

    void recordException(Throwable e);

    void end();
}
