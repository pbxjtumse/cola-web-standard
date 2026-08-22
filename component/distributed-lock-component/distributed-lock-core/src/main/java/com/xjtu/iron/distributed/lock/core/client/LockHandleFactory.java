package com.xjtu.iron.distributed.lock.core.client;

import com.xjtu.iron.distributed.lock.api.model.LockOptions;
import com.xjtu.iron.distributed.lock.core.observability.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.observability.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.observability.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.protocol.LockLease;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;

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
    private final LockWatchdog watchdog;

    public LockHandleFactory(LockEventPublisher eventPublisher, LockEventFactory eventFactory, LockMetricsFacade metricsFacade, LockWatchdog watchdog
) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.eventFactory = Objects.requireNonNull(eventFactory, "eventFactory must not be null");
        this.metricsFacade = Objects.requireNonNull(metricsFacade, "metricsFacade must not be null");
        this.watchdog = Objects.requireNonNull(watchdog, "watchdog must not be null");
    }

    /**
     * 创建最终 Handle，并在 autoRenew=true 时立即启动统一 watchdog。
     *
     * <p>把启动点放在“成功 acquire -> Handle 创建”这个公共节点，而不是 execute 模板里，
     * 可以保证手工 tryLock 和 execute 两种 API 的自动续期语义一致。</p>
     */
    public DefaultLockHandle create(LockProvider provider, LockLease lease, LockOptions options) {
        DefaultLockHandle handle = new DefaultLockHandle(Objects.requireNonNull(provider, "provider must not be null"),
                Objects.requireNonNull(lease, "lease must not be null"), new LockRuntimeState(), eventPublisher, eventFactory, metricsFacade,
                watchdog);
        if (Objects.requireNonNull(options, "options must not be null").isAutoRenew()) {
            watchdog.start(handle, options);
        }
        return handle;
    }
}
