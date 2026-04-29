package com.xjtu.iron.cola.web.tracing;

public class TraceTemplate {

    private final ITraceService traceService;

    public TraceTemplate(ITraceService traceService) {
        this.traceService = traceService;
    }

    public <T, E extends Throwable> T execute(String spanName, ITraceCallback<T, E> callback) throws E {
        ITraceSpan span = traceService.startSpan(spanName);
        try {
            return callback.execute();
        } catch (Throwable e) {
            span.error(e);
            throw e;
        } finally {
            span.close();
        }
    }

    public <T, E extends Throwable> T execute(String spanName, ITraceSpanCallback<T, E> callback) throws E {
        ITraceSpan span = traceService.startSpan(spanName);
        try {
            return callback.execute(span);
        } catch (Throwable e) {
            span.error(e);
            throw e;
        } finally {
            span.close();
        }
    }

    public <E extends Throwable> void executeWithoutResult(String spanName, ITraceRunnable<E> runnable) throws E {
        ITraceSpan span = traceService.startSpan(spanName);
        try {
            runnable.run();
        } catch (Throwable e) {
            span.error(e);
            throw e;
        } finally {
            span.close();
        }
    }
}
