package com.xjtu.iron.distributed.lock.core.client;

import com.xjtu.iron.distributed.lock.api.client.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.client.LockCallback;
import com.xjtu.iron.distributed.lock.api.model.LockHandle;
import com.xjtu.iron.distributed.lock.api.model.LockOptions;
import com.xjtu.iron.distributed.lock.api.model.LockResult;
import com.xjtu.iron.distributed.lock.core.acquire.LockAcquisitionService;
import com.xjtu.iron.distributed.lock.core.execute.LockExecutionTemplate;

import java.util.Objects;

/**
 * 分布式锁客户端默认实现，也是整个 core 对外暴露的门面类。
 *
 * <p>这个类刻意保持很薄：它只把 {@code tryLock} 委托给 {@link LockAcquisitionService}，把 {@code execute} 委托给
 * {@link LockExecutionTemplate}。这样业务入口稳定，内部 acquire、fencing、watchdog、release、metrics 等流程可以独立演进，
 * 不会让 Client 重新变成“上帝类”。</p>
 *
 * <p>新增 Redis、Redisson、Zookeeper、Etcd Provider 时，原则上不应该新增新的 DistributedLockClient 实现；变化点应该落在
 * {@code LockProvider}。只有当整体客户端编排模型发生变化，例如远程锁服务代理、上下文装饰器、测试替身时，才考虑新增 Client 实现。</p>
 */
public final class DefaultDistributedLockClient implements DistributedLockClient {

    private final LockAcquisitionService acquisitionService;
    private final LockExecutionTemplate executionTemplate;

    public DefaultDistributedLockClient(LockAcquisitionService acquisitionService, LockExecutionTemplate executionTemplate) {
        this.acquisitionService = Objects.requireNonNull(acquisitionService, "acquisitionService must not be null");
        this.executionTemplate = Objects.requireNonNull(executionTemplate, "executionTemplate must not be null");
    }

    /**
     * 手工获取锁。成功结果会返回可跨线程传递的 {@link LockHandle}，调用方负责最终 unlock/close。
     */
    @Override
    public LockResult<LockHandle> tryLock(String lockName, LockOptions options) {
        return acquisitionService.tryLock(lockName, options);
    }

    /**
     * 模板式执行。核心保证是：只有 acquire 成功才执行 callback；callback 完成后一定尝试 release，并用统一 LockResult 解释最终状态。
     */
    @Override
    public <T> LockResult<T> execute(String lockName, LockOptions options, LockCallback<T> callback) {
        return executionTemplate.execute(lockName, options, callback);
    }
}
