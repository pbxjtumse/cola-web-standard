package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.tracing.ITraceService;
import com.xjtu.iron.cola.web.tracing.ITraceSpan;
import com.xjtu.iron.cola.web.tracing.Trace;
import com.xjtu.iron.cola.web.tracing.TraceMdc;
import com.xjtu.iron.cola.web.tracing.noop.NoopTraceSpan;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author pangbo
 */
@Aspect
public class TraceAspect {

    private final ITraceService traceService;

    public TraceAspect(ITraceService traceService) {
        this.traceService = Objects.requireNonNull(traceService, "traceService must not be null");
    }

    @Around("@annotation(com.xjtu.iron.cola.web.tracing.Trace)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println(">>> TraceAspect invoked: " + joinPoint.getSignature());

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Trace trace = method.getAnnotation(Trace.class);

        if (trace == null || !trace.enabled()) {
            return joinPoint.proceed();
        }

        String spanName = resolveSpanName(joinPoint, method, trace);

        ITraceSpan span = safeStartSpan(spanName);
        TraceMdc.MdcScope mdcScope = safePutMdc();

        try {
            span.tag("code.namespace", method.getDeclaringClass().getName());
            span.tag("code.function", method.getName());

            if (trace.recordArgs()) {
                Object[] args = joinPoint.getArgs();
                span.tag("method.args.count", args == null ? 0 : args.length);
            }

            return joinPoint.proceed();
        } catch (Throwable throwable) {
            if (trace.recordException()) {
                safeRecordError(span, throwable);
            }
            throw throwable;
        } finally {
            close(span, mdcScope);
        }
    }

    private ITraceSpan safeStartSpan(String spanName) {
        try {
            ITraceSpan span = traceService.startSpan(spanName);
            return span == null ? NoopTraceSpan.INSTANCE : span;
        } catch (Throwable ignored) {
            return NoopTraceSpan.INSTANCE;
        }
    }

    private TraceMdc.MdcScope safePutMdc() {
        try {
            TraceMdc.MdcScope scope = TraceMdc.put(traceService);
            return scope == null ? TraceMdc.MdcScope.noop() : scope;
        } catch (Throwable ignored) {
            return TraceMdc.MdcScope.noop();
        }
    }

    private void safeRecordError(ITraceSpan span, Throwable throwable) {
        try {
            if (span != null) {
                span.error(throwable);
            }
        } catch (Throwable ignored) {
            // tracing 失败不能影响业务异常继续抛出
        }
    }

    private void close(ITraceSpan span, TraceMdc.MdcScope mdcScope) {
        try {
            if (span != null) {
                span.close();
            }
        } finally {
            if (mdcScope != null) {
                mdcScope.close();
            }
        }
    }

    private String resolveSpanName(ProceedingJoinPoint joinPoint, Method method, Trace trace) {
        if (trace.value() != null && !trace.value().isBlank()) {
            return trace.value();
        }

        return joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + method.getName();
    }
}
