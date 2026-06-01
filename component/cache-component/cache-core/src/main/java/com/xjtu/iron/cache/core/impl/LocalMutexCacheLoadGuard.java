package com.xjtu.iron.cache.core.impl;

import com.xjtu.iron.cache.core.CacheLoadGuard;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于本地 JVM 锁的缓存加载保护器。
 *
 * <p>一期用于防止单个应用实例内的缓存击穿。它不能保证多实例之间互斥，
 * 多实例击穿保护需要二期引入 Redis 分布式锁或治理组件。</p>
 */
public class LocalMutexCacheLoadGuard implements CacheLoadGuard {

    /**
     * 每个 key 对应一把本地锁。
     *
     * <p>使用 ConcurrentHashMap 保存锁对象，loader 执行结束后如果没有等待线程则尝试删除锁，
     * 避免锁对象长期堆积。</p>
     */
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /** 使用指定 key 的本地锁包裹 loader 执行。 */
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
