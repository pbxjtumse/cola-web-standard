package com.xjtu.iron.foundation.context;

/**
 * 从外部载体读取执行上下文。
 */
@FunctionalInterface
public interface ContextReader {

    ExecutionContext read(ContextCarrier carrier);
}
