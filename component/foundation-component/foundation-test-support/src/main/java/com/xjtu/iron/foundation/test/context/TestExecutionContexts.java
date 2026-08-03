package com.xjtu.iron.foundation.test.context;

import com.xjtu.iron.foundation.context.ExecutionContext;
import com.xjtu.iron.foundation.context.StandardContextKeys;

/**
 * 创建常用测试执行上下文。
 */
public final class TestExecutionContexts {

    private TestExecutionContexts() {
    }

    public static ExecutionContext withRequestId(String requestId) {
        return ExecutionContext.builder()
                .put(StandardContextKeys.REQUEST_ID, requestId)
                .build();
    }

    public static ExecutionContext complete(String requestId, String correlationId, String tenantId) {
        return ExecutionContext.builder()
                .put(StandardContextKeys.REQUEST_ID, requestId)
                .put(StandardContextKeys.CORRELATION_ID, correlationId)
                .put(StandardContextKeys.TENANT_ID, tenantId)
                .build();
    }
}
