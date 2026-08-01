package com.xjtu.iron.retry.core;

import com.xjtu.iron.retry.api.RetryIdGenerator;
import com.xjtu.iron.retry.api.RetryPolicy;

import java.util.UUID;

/** 使用随机 UUID 生成逻辑重试执行标识。 */
public final class UuidRetryIdGenerator implements RetryIdGenerator {

    /** 生成与操作和策略无关的随机 UUID 文本。 */
    @Override
    public String generate(String operationName, RetryPolicy retryPolicy) {
        return UUID.randomUUID().toString();
    }
}
