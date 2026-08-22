package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.api.model.LockOptions;
import com.xjtu.iron.distributed.lock.core.spi.LockAutoRenewMode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一 watchdog 调度器。
 *
 * <p>它同时支持两种 Provider：</p>
 * <ul>
 *     <li>CORE_MANAGED：例如自研 Redis Lua，每个周期主动调用 handle.renew()；</li>
 *     <li>PROVIDER_MANAGED：例如 Redisson，真正 TTL 续期由 Redisson 内部完成，本类周期 checkHeld()，
 *         并在 maxRenewTime 到达时标记失锁、主动释放，从而给 Provider watchdog 增加统一的上限。</li>
 * </ul>
 *
 * <p>这样 Core 不需要出现 if (providerName == "redisson") 之类的产品分支。</p>
 */
public final class ScheduledLockWatchdog implements LockWatchdog, AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /**
     * 默认构造器：生产环境使用 UTC 系统时钟。
     */
    public ScheduledLockWatchdog() {
        this(Executors.newScheduledThreadPool(1, new WatchdogThreadFactory()), Clock.systemUTC());
    }

    /**
     * Spring/业务装配可只注入 Clock，调度线程仍由组件自己的 ThreadFactory 创建。
     */
    public ScheduledLockWatchdog(Clock clock) {
        this(Executors.newScheduledThreadPool(1, new WatchdogThreadFactory()), clock);
    }

    /**
     * 兼容已有测试/手工装配：调度器由调用方提供，时钟仍使用 UTC 系统时钟。
     */
    public ScheduledLockWatchdog(ScheduledExecutorService scheduler) {
        this(scheduler, Clock.systemUTC());
    }

    /**
     * 完整构造器。Clock 显式注入后，maxRenewTime 的判断可以稳定测试，
     * 也和 Core 其它使用 Clock 的流程保持一致。
     */
    public ScheduledLockWatchdog(ScheduledExecutorService scheduler, Clock clock) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void start(WatchdogLockHandle handle, LockOptions options) {
        Objects.requireNonNull(handle, "handle must not be null");
        Objects.requireNonNull(options, "options must not be null");
        if (!options.isAutoRenew() || handle.autoRenewMode() == LockAutoRenewMode.UNSUPPORTED) {
            return;
        }

        String watchdogId = handle.watchdogId();
        stop(handle);
        Duration interval = options.getRenewInterval();
        Instant maxRenewDeadline = handle.acquiredAt().plus(options.getMaxRenewTime());
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                () -> tick(handle, maxRenewDeadline),
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS);
        tasks.put(watchdogId, future);
    }

    @Override
    public void stop(WatchdogLockHandle handle) {
        if (handle == null) {
            return;
        }
        ScheduledFuture<?> future = tasks.remove(handle.watchdogId());
        if (future != null) {
            future.cancel(false);
        }
    }

    private void tick(WatchdogLockHandle handle, Instant maxRenewDeadline) {
        if (handle.isReleaseAttempted() || handle.isLost()) {
            stop(handle);
            return;
        }

        if (!Instant.now(clock).isBefore(maxRenewDeadline)) {
            /*
             * CORE_MANAGED：停止续期后让底层 leaseTime 自然到期。
             * PROVIDER_MANAGED：Redisson 自己会继续 watchdog，因此必须显式释放才能真正停止续期。
             * 无论哪种模式，都先标记 lost，保证 execute 最终不会把超限执行解释成正常 SUCCESS。
             */
            handle.markLostByWatchdog("maxRenewTime exceeded", null);
            if (handle.autoRenewMode() == LockAutoRenewMode.PROVIDER_MANAGED) {
                handle.unlock();
            }
            stop(handle);
            return;
        }

        if (handle.autoRenewMode() == LockAutoRenewMode.CORE_MANAGED) {
            boolean renewed = handle.renew();
            if (!renewed && handle.isLost()) {
                stop(handle);
            }
            return;
        }

        if (handle.autoRenewMode() == LockAutoRenewMode.PROVIDER_MANAGED) {
            // Provider 自己续期；Core 只检查锁是否仍属于本次 ownerToken。
            boolean held = handle.checkHeld();
            if (!held && handle.isLost()) {
                stop(handle);
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    private static final class WatchdogThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger();
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "iron-lock-watchdog-" + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
