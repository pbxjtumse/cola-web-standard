package com.xjtu.iron.cola.web.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public final class ThreadPoolRegistry {

    private static final Map<String, ExecutorService> POOLS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> TAG_INDEX = new ConcurrentHashMap<>();

    public static void register(String name, ExecutorService pool, Set<String> tags) {
        POOLS.put(name, pool);
        tags.forEach(tag -> TAG_INDEX.computeIfAbsent(tag, k -> new HashSet<>()).add(name));
    }

    public static ExecutorService get(String name) {
        return POOLS.get(name);
    }

    public static List<ExecutorService> getByTag(String tag) {
        return TAG_INDEX.getOrDefault(tag, Set.of())
                .stream()
                .map(POOLS::get)
                .collect(Collectors.toList());
    }

    public static void shutdownAll() {
        POOLS.values().forEach(ExecutorService::shutdown);
    }
}

