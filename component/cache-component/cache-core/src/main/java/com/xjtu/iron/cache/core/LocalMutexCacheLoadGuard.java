package com.xjtu.iron.cache.core;


import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LocalMutexCacheLoadGuard implements CacheLoadGuard {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T execute(String key, Callable<T> loader) throws Exception {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());

        lock.lock();
        try {
            return loader.call();
        } finally {
            lock.unlock();

            if (!lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }
}
