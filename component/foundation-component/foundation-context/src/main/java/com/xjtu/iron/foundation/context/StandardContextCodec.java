package com.xjtu.iron.foundation.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 编解码标准字符串上下文键。
 *
 * <p>未知条目默认忽略，避免把任意不受控 Header 注入内部执行上下文。</p>
 */
public final class StandardContextCodec implements ContextCodec {

    /** 允许由标准编解码器处理的上下文键。 */
    private final Map<String, ContextKey<String>> keys;
    /** 决定标准键是否允许跨边界传播的策略。 */
    private final ContextPropagationPolicy propagationPolicy;

    public StandardContextCodec() {
        this(ContextPropagationPolicy.allowAll());
    }

    public StandardContextCodec(ContextPropagationPolicy propagationPolicy) {
        this.propagationPolicy = java.util.Objects.requireNonNull(
                propagationPolicy, "propagationPolicy must not be null"
        );
        LinkedHashMap<String, ContextKey<String>> configured = new LinkedHashMap<>();
        register(configured, StandardContextKeys.REQUEST_ID);
        register(configured, StandardContextKeys.CORRELATION_ID);
        register(configured, StandardContextKeys.TENANT_ID);
        register(configured, StandardContextKeys.OPERATOR_ID);
        register(configured, StandardContextKeys.TRACE_PARENT);
        this.keys = Map.copyOf(configured);
    }

    private static void register(Map<String, ContextKey<String>> target, ContextKey<String> key) {
        target.put(key.getName(), key);
    }

    @Override
    public ExecutionContext read(ContextCarrier carrier) {
        if (carrier == null) {
            return ExecutionContext.empty();
        }
        ExecutionContextBuilder builder = ExecutionContext.builder();
        keys.forEach((name, key) -> {
            String value = carrier.get(name);
            if (value != null && propagationPolicy.canPropagate(key)) {
                builder.put(key, value);
            }
        });
        return builder.build();
    }

    @Override
    public void write(ExecutionContext context, ContextCarrier carrier) {
        if (context == null || carrier == null) {
            return;
        }
        keys.forEach((name, key) -> context.get(key).ifPresent(value -> {
            if (propagationPolicy.canPropagate(key)) {
                carrier.put(name, value);
            }
        }));
    }
}
