package com.xjtu.iron.cola.web.scaler.impl;

import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import com.xjtu.iron.cola.web.scaler.ThreadPoolScaler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author pangbo
 * @date 2025/12/18
 */
public class ThreadPoolScalerImpl implements ThreadPoolScaler {

    /**
     * @param poolName
     * @param newCoreSize
     */
    @Override
    public void resizeCorePoolSize(String poolName, int newCoreSize) {
        ThreadPoolExecutor executor = ThreadPoolRegistry.getRawExecutor(poolName);
        if (executor == null) {
            throw new IllegalArgumentException("Pool not found: " + poolName);
        }
        executor.setCorePoolSize(newCoreSize);
    }

    /**
     * @param poolName
     * @param newMaxSize
     */
    @Override
    public void resizeMaxPoolSize(String poolName, int newMaxSize) {
        ThreadPoolExecutor executor = ThreadPoolRegistry.getRawExecutor(poolName);
        if (executor == null) {
            throw new IllegalArgumentException("Pool not found: " + poolName);
        }
        executor.setMaximumPoolSize(newMaxSize);
    }

    /**
     * @param poolName
     * @param newCoreSize
     * @param newMaxSize
     */
    @Override
    public void resize(String poolName, int newCoreSize, int newMaxSize) {
        ThreadPoolExecutor executor = ThreadPoolRegistry.getRawExecutor(poolName);
        if (executor == null) {
            throw new IllegalArgumentException("Pool not found: " + poolName);
        }

        // 顺序很重要：max 必须 >= core
        if (newMaxSize < newCoreSize) {
            throw new IllegalArgumentException("maxPoolSize must be >= corePoolSize"
            );
        }
        executor.setMaximumPoolSize(newMaxSize);
        executor.setCorePoolSize(newCoreSize);
    }
}

