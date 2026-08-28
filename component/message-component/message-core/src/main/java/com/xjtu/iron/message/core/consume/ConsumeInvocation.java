package com.xjtu.iron.message.core.consume;

import com.xjtu.iron.message.api.consume.ConsumeDecision;

/**
 * 封装一次业务 Handler 调用，避免幂等执行器直接处理泛型 Handler。
 */
@FunctionalInterface
public interface ConsumeInvocation {
    ConsumeDecision invoke();
}
