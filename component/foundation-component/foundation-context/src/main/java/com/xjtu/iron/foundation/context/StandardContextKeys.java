package com.xjtu.iron.foundation.context;

/**
 * 定义多个技术组件共同理解的低基数标准上下文键。
 */
public final class StandardContextKeys {

    public static final ContextKey<String> REQUEST_ID = ContextKey.of("requestId", String.class);
    public static final ContextKey<String> CORRELATION_ID = ContextKey.of("correlationId", String.class);
    public static final ContextKey<String> TENANT_ID = ContextKey.of("tenantId", String.class);
    public static final ContextKey<String> OPERATOR_ID = ContextKey.of("operatorId", String.class);
    public static final ContextKey<String> TRACE_PARENT = ContextKey.of("traceparent", String.class);

    private StandardContextKeys() {
    }
}
