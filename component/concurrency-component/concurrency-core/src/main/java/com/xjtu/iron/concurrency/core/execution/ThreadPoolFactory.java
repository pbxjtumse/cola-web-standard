package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.ThreadPoolSpec;

import java.util.concurrent.ExecutorService;

/**
 * 线程池工厂。
 */
public interface ThreadPoolFactory {

    ExecutorService create(ThreadPoolSpec spec);
}