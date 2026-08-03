package com.xjtu.iron.foundation.context;

/**
 * 决定某个上下文键是否允许跨边界传播。
 */
@FunctionalInterface
public interface ContextPropagationPolicy {

    boolean canPropagate(ContextKey<?> key);

    static ContextPropagationPolicy allowAll() {
        return key -> true;
    }
}
