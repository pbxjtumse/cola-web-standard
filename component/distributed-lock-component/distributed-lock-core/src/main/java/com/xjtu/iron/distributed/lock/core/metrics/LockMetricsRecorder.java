package com.xjtu.iron.distributed.lock.core.metrics;

import com.xjtu.iron.distributed.lock.api.LockStatus;

import java.time.Duration;

/**
 * 分布式锁指标记录器。
 *
 * <p>该接口用于隔离 Micrometer 等具体指标实现。注意 lockName 应尽量使用归一化 pattern，避免高基数。</p>
 */
public interface LockMetricsRecorder {

    void recordAcquire(String provider, String namespace, String lockNamePattern, boolean success, Duration duration);

    void recordHold(String provider, String namespace, String lockNamePattern, LockStatus status, Duration duration);

    void recordRenew(String provider, String namespace, boolean success);

    void recordRelease(String provider, String namespace, boolean success);

    void recordLost(String provider, String namespace);

    /**
     * 记录 fencing token 生成结果。使用 default 方法保持一期自定义实现兼容。
     */
    default void recordFencing(String lockProvider, String fencingProvider, String namespace,
                               boolean success, Duration duration) {
    }
}
