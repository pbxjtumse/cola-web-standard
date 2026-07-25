package com.xjtu.iron.distributed.lock.core.acquire.outcome;

import com.xjtu.iron.distributed.lock.core.DefaultLockHandle;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.runtime.LockRuntimeState;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.model.LockLease;

import java.util.Objects;

/**
 * 创建 {@link DefaultLockHandle}。
 *
 * <p>这里保留一个具体工厂类，是为了集中隐藏 LockHandle 的运行态、事件和指标依赖；
 * 不再额外维护只有一个实现的 Factory 接口与 Default 实现。</p>
 */
public final class LockHandleFactory {

    private final LockEventPublisher eventPublisher;
    private final LockEventFactory eventFactory;
    private final LockMetricsFacade metricsFacade;

    public LockHandleFactory(
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade
    ) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
    }

    public DefaultLockHandle create(LockProvider provider, LockLease lease) {
        return new DefaultLockHandle(
                Objects.requireNonNull(provider, "provider must not be null"),
                Objects.requireNonNull(lease, "lease must not be null"),
                new LockRuntimeState(),
                eventPublisher,
                eventFactory,
                metricsFacade);
    }
}
