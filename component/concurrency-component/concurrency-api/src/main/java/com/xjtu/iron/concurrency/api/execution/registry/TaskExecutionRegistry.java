package com.xjtu.iron.concurrency.api.execution.registry;

import java.util.List;
import java.util.Optional;

/**
 * 任务执行状态注册表。
 *
 * <p>一期提供本地内存实现，用于按 taskId 查询最近任务状态。二期可扩展 Redis/DB 持久化实现。</p>
 */
public interface TaskExecutionRegistry {

    /** 更新任务状态快照。 */
    void update(TaskExecutionSnapshot snapshot);

    /** 根据 taskId 查询任务状态。 */
    Optional<TaskExecutionSnapshot> get(String taskId);

    /** 查询最近任务状态。 */
    List<TaskExecutionSnapshot> recent(int limit);
}
