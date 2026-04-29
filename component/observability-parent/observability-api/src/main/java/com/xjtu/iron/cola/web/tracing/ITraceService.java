package com.xjtu.iron.cola.web.tracing;

/**
 * @author faywong
 */
public interface ITraceService {

    ITraceSpan startSpan(String name);

    String traceId();

    String spanId();
}
