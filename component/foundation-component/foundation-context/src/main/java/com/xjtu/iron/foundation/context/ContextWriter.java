package com.xjtu.iron.foundation.context;

/**
 * 将执行上下文写入外部载体。
 */
@FunctionalInterface
public interface ContextWriter {

    void write(ExecutionContext context, ContextCarrier carrier);
}
