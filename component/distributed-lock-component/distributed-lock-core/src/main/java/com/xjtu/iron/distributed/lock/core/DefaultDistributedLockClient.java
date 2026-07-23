package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockCallback;
import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.core.acquire.LockAcquisitionService;
import com.xjtu.iron.distributed.lock.core.execute.LockExecutionTemplate;

import java.util.Objects;

/**
 * 默认分布式锁客户端门面。
 *
 * <p>客户端只保留两个稳定入口：获取锁与模板执行。参数准备、Provider 调用、fencing、
 * callback、watchdog、release、事件和指标分别由内部流程对象完成，避免客户端随着功能增加
 * 持续膨胀。</p>
 */
public final class DefaultDistributedLockClient implements DistributedLockClient {

    private final LockAcquisitionService acquisitionService;
    private final LockExecutionTemplate executionTemplate;

    public DefaultDistributedLockClient(LockAcquisitionService acquisitionService, LockExecutionTemplate executionTemplate) {
        this.acquisitionService = Objects.requireNonNull(acquisitionService, "acquisitionService must not be null");
        this.executionTemplate = Objects.requireNonNull(executionTemplate, "executionTemplate must not be null");
    }

    @Override
    public LockResult<LockHandle> tryLock(String lockName, LockOptions options) {
        return acquisitionService.tryLock(lockName, options);
    }

    @Override
    public <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback) {
        return executionTemplate.execute(lockName, options, callback);
    }
}
