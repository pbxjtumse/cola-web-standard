package com.xjtu.iron.cola.web.tracing;

import com.xjtu.iron.cola.web.tracing.noop.NoopTraceSpan;

import java.util.Map;
import java.util.Objects;

/**
 * Trace 模板类。
 *
 * <p>用于统一封装：</p>
 * <pre>
 * startSpan
 * put MDC
 * try
 * catch
 * record error
 * close span
 * restore MDC
 * </pre>
 *
 * <p>核心原则：可观测失败不能影响业务主流程。</p>
 */
public class TraceTemplate {

    private final ITraceService traceService;

    public TraceTemplate(ITraceService traceService) {
        this.traceService = Objects.requireNonNull(traceService, "traceService must not be null");
    }

    /**
     * 执行一段有返回值的业务逻辑。
     */
    public <T, E extends Throwable> T execute(String spanName, ITraceCallback<T, E> callback) throws E {
        ITraceSpan span = safeStartSpan(spanName);
        TraceMdc.MdcScope mdcScope = safePutMdc();

        try {
            return callback.execute();
        } catch (Throwable e) {
            safeRecordError(span, e);
            throw e;
        } finally {
            close(span, mdcScope);
        }
    }

    /**
     * 执行一段有返回值、并允许调用方操作 Span 的业务逻辑。
     */
    public <T, E extends Throwable> T execute(String spanName, ITraceSpanCallback<T, E> callback) throws E {
        ITraceSpan span = safeStartSpan(spanName);
        TraceMdc.MdcScope mdcScope = safePutMdc();

        try {
            return callback.execute(span);
        } catch (Throwable e) {
            safeRecordError(span, e);
            throw e;
        } finally {
            close(span, mdcScope);
        }
    }

    /**
     * 执行一段无返回值的业务逻辑。
     */
    public <E extends Throwable> void executeWithoutResult(String spanName, ITraceRunnable<E> runnable) throws E {
        ITraceSpan span = safeStartSpan(spanName);
        TraceMdc.MdcScope mdcScope = safePutMdc();

        try {
            runnable.run();
        } catch (Throwable e) {
            safeRecordError(span, e);
            throw e;
        } finally {
            close(span, mdcScope);
        }
    }

    /**
     * 执行一段有返回值的业务逻辑，并批量写入标签。
     */
    public <T, E extends Throwable> T execute(
            String spanName,
            Map<String, Object> tags,
            ITraceCallback<T, E> callback
    ) throws E {
        ITraceSpan span = safeStartSpan(spanName);
        TraceMdc.MdcScope mdcScope = safePutMdc();

        try {
            putTags(span, tags);
            return callback.execute();
        } catch (Throwable e) {
            safeRecordError(span, e);
            throw e;
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

    private void putTags(ITraceSpan span, Map<String, Object> tags) {
        if (span == null || tags == null || tags.isEmpty()) {
            return;
        }

        tags.forEach(span::tag);
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
}