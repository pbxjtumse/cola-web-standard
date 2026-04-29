package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.ITraceSpan;
import com.xjtu.iron.cola.web.tracing.Trace;
import com.xjtu.iron.cola.web.tracing.TraceMdc;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * @author pangbo
 */
public class TraceAspect {

    private final ITraceService traceService;

    public TraceAspect(ITraceService traceService) {
        this.traceService = traceService;
    }

    @Around("@annotation(com.xy.observability.api.Trace)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Trace trace = method.getAnnotation(Trace.class);
        String spanName = resolveSpanName(joinPoint, method, trace);
        ITraceSpan span = traceService.startSpan(spanName);
        try {
            span.tag("code.namespace", method.getDeclaringClass().getName());
            span.tag("code.function", method.getName());
            if (trace.recordArgs()) {
                Object[] args = joinPoint.getArgs();
                span.tag("method.args.count", args == null ? 0 : args.length);
            }
            TraceMdc.put(traceService);
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            span.error(throwable);
            throw throwable;
        } finally {
            span.close();
            TraceMdc.clear();
        }
    }

    private String resolveSpanName(ProceedingJoinPoint joinPoint, Method method, Trace trace) {
        if (trace.value() != null && !trace.value().isBlank()) {
            return trace.value();
        }
        return joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + method.getName();
    }
}
