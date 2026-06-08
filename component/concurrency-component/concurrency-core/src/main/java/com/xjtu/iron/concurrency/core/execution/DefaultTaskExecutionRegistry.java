package com.xjtu.iron.concurrency.core.execution;

import com.xjtu.iron.concurrency.api.execution.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.TaskExecutionSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于内存的任务状态注册表。
 *
 * <p>一期用于本机最近任务诊断；二期可以替换成 Redis 或 DB 实现。</p>
 */
public class DefaultTaskExecutionRegistry implements TaskExecutionRegistry {

    private final ConcurrentMap<String, TaskExecutionSnapshot> snapshots = new ConcurrentHashMap<>();
    private final int maxSize;

    public DefaultTaskExecutionRegistry() {
        this(10_000);
    }

    public DefaultTaskExecutionRegistry(int maxSize) {
        this.maxSize = Math.max(maxSize, 100);
    }

    @Override
    public void update(TaskExecutionSnapshot snapshot) {
        if (snapshot == null || snapshot.getTaskId() == null) {
            return;
        }
        snapshots.put(snapshot.getTaskId(), snapshot);
        evictIfNecessary();
    }

    @Override
    public Optional<TaskExecutionSnapshot> get(String taskId) {
        return Optional.ofNullable(snapshots.get(taskId));
    }

    @Override
    public List<TaskExecutionSnapshot> recent(int limit) {
        int actualLimit = limit <= 0 ? 100 : limit;
        return snapshots.values().stream()
                .sorted(Comparator.comparingLong(TaskExecutionSnapshot::getSubmitTimeMillis).reversed())
                .limit(actualLimit)
                .toList();
    }

    private void evictIfNecessary() {
        if (snapshots.size() <= maxSize) {
            return;
        }
        List<TaskExecutionSnapshot> ordered = new ArrayList<>(snapshots.values());
        ordered.sort(Comparator.comparingLong(TaskExecutionSnapshot::getSubmitTimeMillis));
        int removeCount = Math.max(1, snapshots.size() - maxSize);
        for (int i = 0; i < removeCount && i < ordered.size(); i++) {
            snapshots.remove(ordered.get(i).getTaskId());
        }
    }
}
