package com.xjtu.iron.cola.web.tracing;

/**
 * @author pangbo
 */
@FunctionalInterface
public interface ITraceSpanCallback<T, E extends Throwable> {
    T execute(ITraceSpan span) throws E;
}