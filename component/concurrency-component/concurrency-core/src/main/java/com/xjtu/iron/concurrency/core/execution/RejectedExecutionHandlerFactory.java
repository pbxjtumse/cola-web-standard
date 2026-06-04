package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.RejectedExecutionHandler;

/**
 * 拒绝策略工厂。
 */
public interface RejectedExecutionHandlerFactory {

    RejectedExecutionHandler create(ThreadPoolSpec spec);
}
