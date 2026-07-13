package com.xjtu.iron.distributed.lock.core.watchdog;

import com.xjtu.iron.distributed.lock.api.LockOptions;

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
 * 基于 ScheduledExecutorService 的 watchdog 实现。
 *
 * <p>每个开启 autoRenew 的 LockHandle 会注册一个周期性续期任务。任务会在 renewInterval 到达时调用
 * handle.renew()，续期成功则继续，确定失锁则停止。达到 maxRenewTime 时也会停止续期并把 handle 标记为 lost，
 * 防止业务卡死后无限续期。</p>
 */
public final class ScheduledLockWatchdog implements LockWatchdog, AutoCloseable {

    /** 续期调度线程池。 */
    private final ScheduledExecutorService scheduler;

    /** watchdogId 到续期任务的映射。 */
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public ScheduledLockWatchdog() {
        this(Executors.newScheduledThreadPool(1, new WatchdogThreadFactory()));
    }

    public ScheduledLockWatchdog(ScheduledExecutorService scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    @Override
    public void start(WatchdogLockHandle handle, LockOptions options) {
        Objects.requireNonNull(handle, "handle must not be null");
        Objects.requireNonNull(options, "options must not be null");
        if (!options.isAutoRenew()) {
            return;
        }
        String watchdogId = handle.watchdogId();
        stop(handle);
        Duration interval = options.getRenewInterval();
        Instant maxRenewDeadline = handle.acquiredAt().plus(options.getMaxRenewTime());
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> renewOnce(handle, maxRenewDeadline),
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
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

    private void renewOnce(WatchdogLockHandle handle, Instant maxRenewDeadline) {
        if (handle.isReleased() || handle.isLost()) {
            stop(handle);
            return;
        }
        if (!Instant.now().isBefore(maxRenewDeadline)) {
            handle.markLostByWatchdog("maxRenewTime exceeded", null);
            stop(handle);
            return;
        }
        boolean renewed = handle.renew();
        if (!renewed && handle.isLost()) {
            stop(handle);
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
