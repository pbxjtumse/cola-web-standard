package com.xjtu.iron.distributed.lock.starter.configuration;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.core.DefaultDistributedLockClient;
import com.xjtu.iron.distributed.lock.core.acquire.LockAcquisitionService;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.AcquiredLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.DefaultLockAcquireOutcomeHandlerRegistry;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.LockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.LockAcquireOutcomeHandlerRegistry;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.LockHandleFactory;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.NotAcquiredLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.acquire.outcome.ProviderErrorLockAcquireOutcomeHandler;
import com.xjtu.iron.distributed.lock.core.event.LockEventFactory;
import com.xjtu.iron.distributed.lock.core.execute.LockExecutionTemplate;
import com.xjtu.iron.distributed.lock.core.event.LockEventPublisher;
import com.xjtu.iron.distributed.lock.core.fencing.DefaultFencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenCoordinator;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProvider;
import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenProviderRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.flow.DefaultFencingTokenFlowRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.flow.ExternalFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlowRegistry;
import com.xjtu.iron.distributed.lock.core.fencing.flow.FencingTokenFlowSupport;
import com.xjtu.iron.distributed.lock.core.fencing.flow.NativeFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.fencing.flow.NoFencingTokenFlow;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsFacade;
import com.xjtu.iron.distributed.lock.core.metrics.LockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.metrics.NoOpLockMetricsRecorder;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.DefaultLockNameValidator;
import com.xjtu.iron.distributed.lock.core.name.LockNamePatternResolver;
import com.xjtu.iron.distributed.lock.core.name.LockNameValidator;
import com.xjtu.iron.distributed.lock.core.registry.DefaultLockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.result.LockResultResolver;
import com.xjtu.iron.distributed.lock.core.spi.LockProvider;
import com.xjtu.iron.distributed.lock.core.spi.LockProviderRegistry;
import com.xjtu.iron.distributed.lock.core.token.DefaultOwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.token.OwnerTokenGenerator;
import com.xjtu.iron.distributed.lock.core.wait.LockWaiterFactory;
import com.xjtu.iron.distributed.lock.core.watchdog.LockWatchdog;
import com.xjtu.iron.distributed.lock.core.watchdog.ScheduledLockWatchdog;
import com.xjtu.iron.distributed.lock.starter.event.SpringLockEventPublisher;
import com.xjtu.iron.distributed.lock.starter.metrics.MicrometerLockMetricsRecorder;
import com.xjtu.iron.distributed.lock.starter.properties.DistributedLockProperties;
import com.xjtu.iron.distributed.lock.starter.properties.JdbcFencingTokenProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.Clock;
import java.util.List;

/** 分布式锁核心自动配置。 */
@AutoConfiguration(after = {RedisDistributedLockAutoConfiguration.class, JdbcFencingTokenAutoConfiguration.class})
@EnableConfigurationProperties({DistributedLockProperties.class, JdbcFencingTokenProperties.class})
@ConditionalOnProperty(prefix = "xjtu.iron.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OwnerTokenGenerator ownerTokenGenerator() { return new DefaultOwnerTokenGenerator(); }

    @Bean
    @ConditionalOnMissingBean
    public LockWaiterFactory lockWaiterFactory() { return new LockWaiterFactory(); }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public LockWatchdog lockWatchdog() { return new ScheduledLockWatchdog(); }

    @Bean
    @ConditionalOnMissingBean
    public LockNameValidator lockNameValidator() { return new DefaultLockNameValidator(); }

    @Bean
    @ConditionalOnMissingBean
    public LockNamePatternResolver lockNamePatternResolver() { return new DefaultLockNamePatternResolver(); }

    @Bean
    @ConditionalOnMissingBean
    public LockEventPublisher lockEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new SpringLockEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public LockMetricsRecorder micrometerLockMetricsRecorder(MeterRegistry meterRegistry) {
        return new MicrometerLockMetricsRecorder(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(LockMetricsRecorder.class)
    public LockMetricsRecorder noOpLockMetricsRecorder() { return new NoOpLockMetricsRecorder(); }

    @Bean
    @ConditionalOnMissingBean
    public LockEventFactory lockEventFactory() { return new LockEventFactory(); }

    @Bean
    @ConditionalOnMissingBean
    public LockMetricsFacade lockMetricsFacade(
            LockMetricsRecorder metricsRecorder,
            LockNamePatternResolver patternResolver
    ) {
        return new LockMetricsFacade(metricsRecorder, patternResolver);
    }

    @Bean(name = "distributedLockDefaultOptions")
    @ConditionalOnMissingBean(name = "distributedLockDefaultOptions")
    public LockOptions distributedLockDefaultOptions(DistributedLockProperties properties) {
        return properties.toLockOptions();
    }

    @Bean
    @ConditionalOnBean(LockProvider.class)
    @ConditionalOnMissingBean
    public LockProviderRegistry lockProviderRegistry(List<LockProvider> providers, DistributedLockProperties properties) {
        return new DefaultLockProviderRegistry(properties.getDefaultProvider(), providers);
    }


    @Bean
    @ConditionalOnMissingBean
    public FencingTokenProviderRegistry fencingTokenProviderRegistry(
            List<FencingTokenProvider> providers,
            DistributedLockProperties properties
    ) {
        String configuredName = properties.getFencingTokenProviderName();
        boolean configuredExternalProvider = configuredName != null
                && providers.stream().anyMatch(provider -> configuredName.equals(provider.providerName()));
        return new DefaultFencingTokenProviderRegistry(
                configuredExternalProvider ? configuredName : null, providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public FencingTokenCoordinator fencingTokenCoordinator(
            FencingTokenProviderRegistry registry
    ) {
        return new FencingTokenCoordinator(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public FencingTokenFlowSupport fencingTokenFlowSupport(
            FencingTokenCoordinator fencingTokenCoordinator,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade,
            ObjectProvider<Clock> clockProvider
    ) {
        return new FencingTokenFlowSupport(fencingTokenCoordinator, eventPublisher, eventFactory, metricsFacade,
                clockProvider.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    @ConditionalOnMissingBean(NoFencingTokenFlow.class)
    public NoFencingTokenFlow noFencingTokenFlow() { return new NoFencingTokenFlow(); }

    @Bean
    @ConditionalOnMissingBean(NativeFencingTokenFlow.class)
    public NativeFencingTokenFlow nativeFencingTokenFlow(FencingTokenFlowSupport support) {
        return new NativeFencingTokenFlow(support);
    }

    @Bean
    @ConditionalOnMissingBean(ExternalFencingTokenFlow.class)
    public ExternalFencingTokenFlow externalFencingTokenFlow(FencingTokenFlowSupport support) {
        return new ExternalFencingTokenFlow(support);
    }

    @Bean
    @ConditionalOnMissingBean
    public FencingTokenFlowRegistry fencingTokenFlowRegistry(List<FencingTokenFlow> flows) {
        return new DefaultFencingTokenFlowRegistry(flows);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockHandleFactory lockHandleFactory(LockEventPublisher eventPublisher, LockEventFactory eventFactory,
                                                LockMetricsFacade metricsFacade) {
        return new LockHandleFactory(eventPublisher, eventFactory, metricsFacade);
    }

    @Bean
    @ConditionalOnMissingBean(AcquiredLockAcquireOutcomeHandler.class)
    public AcquiredLockAcquireOutcomeHandler acquiredLockAcquireOutcomeHandler(
            FencingTokenFlowRegistry flowRegistry, LockHandleFactory lockHandleFactory,
            LockEventPublisher eventPublisher, LockEventFactory eventFactory, LockMetricsFacade metricsFacade) {
        return new AcquiredLockAcquireOutcomeHandler(flowRegistry, lockHandleFactory, eventPublisher, eventFactory, metricsFacade);
    }

    @Bean
    @ConditionalOnMissingBean(NotAcquiredLockAcquireOutcomeHandler.class)
    public NotAcquiredLockAcquireOutcomeHandler notAcquiredLockAcquireOutcomeHandler(
            LockEventPublisher eventPublisher, LockEventFactory eventFactory, LockMetricsFacade metricsFacade) {
        return new NotAcquiredLockAcquireOutcomeHandler(eventPublisher, eventFactory, metricsFacade);
    }

    @Bean
    @ConditionalOnMissingBean(ProviderErrorLockAcquireOutcomeHandler.class)
    public ProviderErrorLockAcquireOutcomeHandler providerErrorLockAcquireOutcomeHandler(
            LockEventPublisher eventPublisher, LockEventFactory eventFactory, LockMetricsFacade metricsFacade) {
        return new ProviderErrorLockAcquireOutcomeHandler(eventPublisher, eventFactory, metricsFacade);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockAcquireOutcomeHandlerRegistry lockAcquireOutcomeHandlerRegistry(
            List<LockAcquireOutcomeHandler> handlers) {
        return new DefaultLockAcquireOutcomeHandlerRegistry(handlers);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockResultResolver lockResultResolver() {
        return new LockResultResolver();
    }

    @Bean
    @ConditionalOnBean(LockProviderRegistry.class)
    @ConditionalOnMissingBean
    public LockAcquisitionService lockAcquisitionService(
            LockProviderRegistry providerRegistry,
            OwnerTokenGenerator ownerTokenGenerator,
            LockWaiterFactory waiterFactory,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockNameValidator lockNameValidator,
            @Qualifier("distributedLockDefaultOptions") LockOptions defaultOptions,
            FencingTokenCoordinator fencingTokenCoordinator,
            LockAcquireOutcomeHandlerRegistry acquireOutcomeHandlerRegistry,
            ObjectProvider<Clock> clockProvider
    ) {
        return new LockAcquisitionService(
                providerRegistry,
                ownerTokenGenerator,
                waiterFactory,
                eventPublisher,
                eventFactory,
                lockNameValidator,
                defaultOptions,
                clockProvider.getIfAvailable(Clock::systemUTC),
                fencingTokenCoordinator,
                acquireOutcomeHandlerRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockExecutionTemplate lockExecutionTemplate(
            LockAcquisitionService acquisitionService,
            LockWatchdog watchdog,
            LockEventPublisher eventPublisher,
            LockEventFactory eventFactory,
            LockMetricsFacade metricsFacade,
            LockResultResolver resultResolver,
            ObjectProvider<Clock> clockProvider
    ) {
        return new LockExecutionTemplate(
                acquisitionService,
                watchdog,
                eventPublisher,
                eventFactory,
                metricsFacade,
                clockProvider.getIfAvailable(Clock::systemUTC),
                resultResolver);
    }

    @Bean
    @ConditionalOnBean(LockProviderRegistry.class)
    @ConditionalOnMissingBean
    public DistributedLockClient distributedLockClient(
            LockAcquisitionService acquisitionService,
            LockExecutionTemplate executionTemplate
    ) {
        return new DefaultDistributedLockClient(acquisitionService, executionTemplate);
    }
}
