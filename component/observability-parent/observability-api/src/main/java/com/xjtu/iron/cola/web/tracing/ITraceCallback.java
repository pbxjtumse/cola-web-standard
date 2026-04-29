package com.xjtu.iron.cola.web.tracing;


/**
 * @author pangbo
 */
@FunctionalInterface
public interface ITraceCallback<T, E extends Throwable> {
    T execute() throws E;
}
