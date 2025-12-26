package com.xjtu.iron.cola.web.impl.bulk.semaphore;


import com.xjtu.iron.cola.web.Bulkhead;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pbxjt
 * @date 2025/12/26
 */
@Component
public class SemaphoreBulkhead implements Bulkhead {
    /**
     *
     */
    private final AdjustableSemaphore semaphore;
    /**
     *
     */
    private final AtomicInteger limit = new AtomicInteger();

    /**
     * @param initialLimit
     */
    public SemaphoreBulkhead(int initialLimit) {
        this.limit.set(initialLimit);
        this.semaphore = new AdjustableSemaphore(initialLimit);
    }

    /**
     * @return boolean
     */
    @Override
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    /**
     *
     */
    @Override
    public void release() {
        semaphore.release();
    }

    /**
     * @return int
     */
    @Override
    public int getLimit() {
        return limit.get();
    }

    /**
     * @return int
     */
    @Override
    public int getInUse() {
        return limit.get() - semaphore.availablePermits();
    }

    /**
     * @param newLimit
     */
    @Override
    public synchronized void updateLimit(int newLimit) {
        int oldLimit = this.limit.get();
        int delta = newLimit - oldLimit;

        if (delta > 0) {
            semaphore.release(delta);
        } else if (delta < 0) {
            semaphore.reduce(-delta);
        }

        this.limit.set(newLimit);
    }

}


