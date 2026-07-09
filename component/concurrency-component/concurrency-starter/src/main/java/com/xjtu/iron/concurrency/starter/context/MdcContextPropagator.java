package com.xjtu.iron.concurrency.starter.context;

import com.xjtu.iron.concurrency.api.context.ContextPropagator;
import com.xjtu.iron.concurrency.api.context.ContextSnapshot;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDC 上下文传播器。
 */
public class MdcContextPropagator implements ContextPropagator {

    @Override
    public ContextSnapshot capture() {
        Map<String, String> captured = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (captured == null || captured.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(captured);
            }
            return () -> {
                if (previous == null || previous.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            };
        };
    }
}
