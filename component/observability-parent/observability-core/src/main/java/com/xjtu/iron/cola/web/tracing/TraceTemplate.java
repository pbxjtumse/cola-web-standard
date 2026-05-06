package com.xjtu.iron.cola.web.tracing;

import com.xjtu.iron.cola.web.tracing.noop.NoopTraceSpan;
import com.xjtu.iron.cola.web.tracing.template.ITraceCallback;
import com.xjtu.iron.cola.web.tracing.template.ITraceRunnable;
import com.xjtu.iron.cola.web.tracing.template.ITraceSpanCallback;

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

    /**
     * 是否启用模板级 MDC。
     *
     * <p>开启后，TraceTemplate 创建代码块级 Span 后，
     * 会把该 Span 的 traceId/spanId 临时写入 MDC。</p>
     */
    private final boolean mdcEnabled;

    public TraceTemplate(ITraceService traceService) {
        this(traceService, true);
    }

    public TraceTemplate(ITraceService traceService, boolean mdcEnabled) {
        this.traceService = Objects.requireNonNull(traceService, "traceService must not be null");
        this.mdcEnabled = mdcEnabled;
    }

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
        if (!mdcEnabled) {
            return TraceMdc.MdcScope.noop();
        }

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