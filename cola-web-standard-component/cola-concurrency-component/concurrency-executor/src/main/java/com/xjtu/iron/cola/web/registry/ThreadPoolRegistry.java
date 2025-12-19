package com.xjtu.iron.cola.web.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author pangbo
 * @date 2025/12/18
 */
public final class ThreadPoolRegistry {

    /**
     * 线程池实例 Map集合
     */
    private static final Map<String, ExecutorService> POOLS = new ConcurrentHashMap<>();
    /**
     * 线程池Tag Map集合
     */
    private static final Map<String, Set<String>> TAG_INDEX = new ConcurrentHashMap<>();

    /**
     *含义： TAG_INDEX 在治理里的真实价值是 TAG_INDEX = 线程池的语义分组
     *存储方式：注册线程池到 POOLS 中 然后并对标签map 存储
     *  pool_name → ExecutorService
     *  tag → poolNameList
     *使用方式：可以使用横切管理 能够 impl.limitByTag("mq", maxConcurrency = 50);
     *   所有tag = mq的线程池、限制总并发数不超50 超出->拒绝或者降级，
     *   在executor 不做治理决策 在治理模块做 决定是否提交  决定是否降级  决定是否拒绝
     *   App -> Governance（限流 / 舱壁）-> Executor
     * @param name 线程名称
     * @param pool 线程池实例
     * @param tags 线程标签
     */
    public static void register(String name, ExecutorService pool, Set<String> tags) {
        POOLS.put(name, pool);
        tags.forEach(tag -> TAG_INDEX.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(name));
    }

    /**
     * @param name
     * @return {@link ExecutorService }
     */
    public static ExecutorService get(String name) {
        return POOLS.get(name);
    }

    /**
     * 根据标签获取对应的线程池集合
     * @param tag 标签纸
     * @return {@link List }<{@link ExecutorService }>
     */
    public static List<ExecutorService> getByTag(String tag) {
        Set<String> names = TAG_INDEX.get(tag);
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return names.stream()
                .map(POOLS::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static ThreadPoolExecutor getRawExecutor(String name) {
        ExecutorService executor = POOLS.get(name);
        if (executor == null) {
            return null;
        }
        if (executor instanceof ThreadPoolExecutor) {
            return (ThreadPoolExecutor) executor;
        }
        throw new IllegalStateException(
                "Executor [" + name + "] is not a ThreadPoolExecutor"
        );
    }

    /**
     *
     */
    public static void shutdownAll() {
        POOLS.values().forEach(ExecutorService::shutdown);
    }
}

