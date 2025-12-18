package com.xjtu.iron.cola.web.governor;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.exception.GovernanceRejectedException;
import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * @author pangbo
 * @date 2025/12/19
 */
@Component
public class BulkheadConcurrencyGovernor implements ConcurrencyGovernor {

    /**
     *
     */
    private final Map<String, Semaphore> bulkheads = new ConcurrentHashMap<>();

    /**
     * @param poolName
     * @param maxConcurrency
     */
    public void register(String poolName, int maxConcurrency) {
        bulkheads.put(poolName, new Semaphore(maxConcurrency));
    }

    /**
     * @param poolName
     * @param task
     */
    @Override
    public void execute(String poolName, Runnable task) {
        ExecutorService executor = ThreadPoolRegistry.get(poolName);
        Semaphore semaphore = bulkheads.get(poolName);
        if (semaphore == null) {
            submit(executor, task);
            return;
        }
        if (!semaphore.tryAcquire()) {
            // 这里才是“真正的拒绝处理点”
            throw new GovernanceRejectedException("Bulkhead limit reached: " + poolName);
        }
        submit(executor, () -> {
            try {
                task.run();
            } finally {
                semaphore.release();
            }
        });
    }

    /**
     * @param executor
     * @param task
     */
    private void submit(ExecutorService executor, Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            // 线程池层拒绝（兜底）
            throw ex;
        }
    }
}

