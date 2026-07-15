package com.xjtu.iron.distributed.lock.core.event;

import com.xjtu.iron.distributed.lock.api.LockStage;
import com.xjtu.iron.distributed.lock.api.LockStatus;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;
import com.xjtu.iron.distributed.lock.core.spi.request.LockAcquireRequest;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;

/** 统一创建 LockEvent，避免事件字段在 client/handle 中重复拼装。 */
public final class LockEventFactory {

    public LockEvent fromLease(LockLease lease, LockEventType type, LockStage stage, LockStatus status, Throwable error) {
        return LockEvent.builder()
                .eventType(type)
                .stage(stage)
                .status(status)
                .namespace(lease.getNamespace())
                .lockName(lease.getLockName())
                .lockKey(lease.getLockKey())
                .providerName(lease.getProviderName())
                .ownerToken(lease.getOwnerToken())
                .fencingToken(lease.fencingToken().isPresent() ? lease.fencingToken().getAsLong() : null)
                .error(error)
                .build();
    }

    public LockEvent fromAcquireRequest(LockProvider provider, LockAcquireRequest request,
                                        LockEventType type, LockStage stage, LockStatus status, Throwable error) {
        return LockEvent.builder()
                .eventType(type)
                .stage(stage)
                .status(status)
                .namespace(request.getNamespace())
                .lockName(request.getLockName())
                .providerName(provider.providerName())
                .ownerToken(request.getOwnerToken())
                .error(error)
                .build();
    }
}
