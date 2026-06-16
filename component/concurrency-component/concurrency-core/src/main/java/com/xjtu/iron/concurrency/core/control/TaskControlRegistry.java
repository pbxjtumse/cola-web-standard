package com.xjtu.iron.concurrency.core.control;

import java.util.Optional;

/**
 * 当前 JVM 运行任务控制注册表。
 *
 * <p>
 * 只保存尚未进入最终状态的任务控制对象；任务完成后必须及时移除，避免长期持有 ThreadPoolExecutor、
 * TaskCommand 和 Future。
 * </p>
 */
public interface TaskControlRegistry {

    /** 注册运行任务控制对象。 */
    void register(String taskId, CancellableTask task);

    /** 查询运行任务控制对象。 */
    Optional<CancellableTask> get(String taskId);

    /** 在对象仍然匹配时移除，避免旧任务误删同 taskId 的新执行实例。 */
    void remove(String taskId, CancellableTask task);
}
