package com.xjtu.iron.cola.web.tracing;

public interface ITraceSpan extends AutoCloseable{
    void tag(String key, String value);

    void tag(String key, long value);

    void tag(String key, double value);

    void tag(String key, boolean value);

    void error(Throwable throwable);

    @Override
    void close();
}
