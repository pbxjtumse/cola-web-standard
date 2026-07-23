package com.xjtu.iron.distributed.lock.core;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.acquire.LockAcquisitionService;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.AcquiredLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.DefaultLockAcquireOutcomeHandlerRegistry;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.LockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.LockHandleFactory;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.NotAcquiredLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.ProviderErrorLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.event.NoOpLockEventPublisher;
import com.xjtu.iron.distributed.lock.core.execute.LockExecutionTemplate;
import com.xjtu.iron.distributed.lock.core.fencing.DefaultFencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenCoordinator;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProvider;
import com.xjtu.iron.distributed.lock.core.fencing.flow.DefaultFencingTokenFlowRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.flow.ExternalFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlowRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlowSupport;
import com.xjtu.iron.distributed.lock.core.fencing.flow.NativeFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.NoFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.metrics.NoOpLockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNameValidator;
import com.xjtu.iron.distributed.lock.core.registry.DefaultLockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.result.LockResultResolver;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.token.DefaultOwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;
import com.xjtu.iron.distributed.lock.core.watchdog.NoOpLockWatchdog;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 测试侧装配工具，避免为了兼容测试在生产 Client 上保留大量构造函数。 */
final class TestDistributedLockClients {

    private TestDistributedLockClients() {
    }

    static DistributedLockClient create(LockProvider lockProvider) {
        return create(lockProvider, Collections.emptyList());
    }

    static DistributedLockClient create(
            LockProvider lockProvider,
            List<FencingTokenProvider> fencingProviders
    ) {
        Clock clock = Clock.systemUTC();
        NoOpLockEventPublisher eventPublisher = new NoOpLockEventPublisher();
        LockEventFactory eventFactory = new LockEventFactory();
        LockMetricsFacade metricsFacade = new LockMetricsFacade(
                new NoOpLockMetricsRecorder(),
                new DefaultLockNamePatternResolver());

        FencingTokenCoordinator coordinator = new FencingTokenCoordinator(
                new DefaultFencingTokenProviderRegistry(fencingProviders));
        FencingTokenFlowSupport flowSupport = new FencingTokenFlowSupport(
                coordinator,
                eventPublisher,
                eventFactory,
                metricsFacade,
                clock);
        List<FencingTokenFlow> flows = Arrays.asList(
                new NoFencingTokenFlow(),
                new NativeFencingTokenFlow(flowSupport),
                new ExternalFencingTokenFlow(flowSupport));
        FencingTokenFlowRegistry flowRegistry = new DefaultFencingTokenFlowRegistry(flows);

        LockHandleFactory handleFactory = new LockHandleFactory(
                eventPublisher,
                eventFactory,
                metricsFacade);
        List<LockAcquireOutcomeHandler> handlers = Arrays.asList(
                new AcquiredLockAcquireOutcomeHandler(
                        flowRegistry,
                        handleFactory,
                        eventPublisher,
                        eventFactory,
                        metricsFacade),
                new NotAcquiredLockAcquireOutcomeHandler(
                        eventPublisher,
                        eventFactory,
                        metricsFacade),
                new ProviderErrorLockAcquireOutcomeHandler(
                        eventPublisher,
                        eventFactory,
                        metricsFacade));

        LockAcquisitionService acquisitionService = new LockAcquisitionService(
                new DefaultLockProviderRegistry("redis", Collections.singletonList(lockProvider)),
                new DefaultOwnerTokenGenerator(),
                new LockWaiterFactory(),
                eventPublisher,
                eventFactory,
                new DefaultLockNameValidator(),
                LockOptions.defaults(),
                clock,
                coordinator,
                new DefaultLockAcquireOutcomeHandlerRegistry(handlers));

        LockExecutionTemplate executionTemplate = new LockExecutionTemplate(
                acquisitionService,
                new NoOpLockWatchdog(),
                eventPublisher,
                eventFactory,
                metricsFacade,
                clock,
                new LockResultResolver());

        return new DefaultDistributedLockClient(acquisitionService, executionTemplate);
    }
}
