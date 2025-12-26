package com.xjtu.iron.config;

import com.xjtu.iron.cola.web.ConcurrencyGovernor;
import com.xjtu.iron.cola.web.GovernanceExecutor;
import com.xjtu.iron.cola.web.dto.TagRule;
import com.xjtu.iron.cola.web.impl.bulk.BulkheadRegistry;
import com.xjtu.iron.cola.web.impl.bulk.semaphore.SemaphoreBulkhead;
import com.xjtu.iron.cola.web.impl.executor.DefaultGovernanceExecutor;
import com.xjtu.iron.cola.web.impl.governor.BulkheadGovernor;
import org.springframework.context.annotation.Bean;

public class GovernanceConfig {
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = new BulkheadRegistry();

        registry.register(TagRule.builder().api("order.create").build(),
                new SemaphoreBulkhead(50)
        );

        registry.register(
                TagRule.builder().api("order.create").tenant("vip").build(),
                new SemaphoreBulkhead(20)
        );

        return registry;
    }

    @Bean
    public ConcurrencyGovernor concurrencyGovernor(BulkheadRegistry registry) {
        return new BulkheadGovernor(registry);
    }

    @Bean
    public GovernanceExecutor governanceExecutor(ConcurrencyGovernor governor) {
        return new DefaultGovernanceExecutor(governor);
    }
}
