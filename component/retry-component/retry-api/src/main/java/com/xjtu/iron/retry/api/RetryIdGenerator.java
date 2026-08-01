package com.xjtu.iron.retry.api;

/** 为每次逻辑重试执行生成稳定标识。 */
@FunctionalInterface
public interface RetryIdGenerator {

    /**
     * 生成一次逻辑执行的标识。
     *
     * @param operationName 操作名称
     * @param retryPolicy 已解析的重试策略
     * @return 非空且非空白的重试标识
     */
    String generate(String operationName, RetryPolicy retryPolicy);
}
