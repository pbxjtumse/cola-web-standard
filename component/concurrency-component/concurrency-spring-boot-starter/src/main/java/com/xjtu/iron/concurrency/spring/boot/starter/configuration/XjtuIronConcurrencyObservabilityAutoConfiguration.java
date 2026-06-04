package com.xjtu.iron.concurrency.spring.boot.starter.configuration;

import com.xjtu.iron.concurrency.core.execution.ThreadPoolRegistry;
import com.xjtu.iron.concurrency.core.metrics.ConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.metrics.NoopConcurrencyMetricsRecorder;
import com.xjtu.iron.concurrency.core.metrics.ThreadPoolMetricName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 并行组件可观测性自动装配。
 *
 * <p>这个配置类负责两件事：</p>
 * <ol>
 *     <li>提供默认的并发指标记录器。</li>
 *     <li>把每个 ThreadPoolExecutor 的运行状态注册成 Micrometer Gauge。</li>
 * </ol>
 *
 * <p>指标名称由 {@link ThreadPoolMetricName} 统一维护，避免监控指标散落在配置类里。</p>
 */
@AutoConfiguration(after = XjtuIronConcurrencyExecutionAutoConfiguration.class)
public class XjtuIronConcurrencyObservabilityAutoConfiguration {

    /**
     * 创建默认指标记录器。
     *
     * <p>如果业务侧没有引入真正的 Micrometer 记录器，就使用 Noop 实现，保证组件可独立运行。</p>
     *
     * @return 并发指标记录器
     */
    @Bean
    @ConditionalOnMissingBean
    public ConcurrencyMetricsRecorder concurrencyMetricsRecorder() {
        return new NoopConcurrencyMetricsRecorder();
    }

    /**
     * 注册线程池 Gauge 指标。
     *
     * <p>这里使用 {@link ThreadPoolRegistry#snapshot()} 拿到所有已注册线程池，
     * 然后给每个线程池注册 active、queue、completed 等运行时指标。</p>
     *
     * @param threadPoolRegistry 线程池注册中心
     * @return Micrometer MeterBinder
     */
    @Bean(name = "ironConcurrencyThreadPoolMeterBinder")
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnMissingBean(name = "ironConcurrencyThreadPoolMeterBinder")
    public MeterBinder threadPoolMeterBinder(ThreadPoolRegistry threadPoolRegistry) {
        return meterRegistry -> threadPoolRegistry.snapshot().forEach((poolName, executor) -> {
            Tags tags = Tags.of(
                    "component", "xjtu-iron-concurrency",
                    "pool", poolName
            );

            for (ThreadPoolMetricName metricName : ThreadPoolMetricName.values()) {
                Gauge.builder(metricName.meterName(), executor, metricName::value)
                        .description(metricName.description())
                        .tags(tags)
                        .register(meterRegistry);
            }
        });
    }
}
