package com.xjtu.iron.cola.web.impl.bulk.semaphore;

import java.util.concurrent.Semaphore;

public class AdjustableSemaphore extends Semaphore {
    public AdjustableSemaphore(int permits) {
        super(permits);
    }

    public synchronized void reduce(int reduction) {
        super.reducePermits(reduction);
    }
}
