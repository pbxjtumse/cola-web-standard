package com.xjtu.iron.config;

import com.xjtu.iron.ConcurrencyGovernor;
import com.xjtu.iron.GovernanceExecutor;
import com.xjtu.iron.dto.TagRule;
import com.xjtu.iron.bulk.BulkheadRegistry;
import com.xjtu.iron.bulk.semaphore.SemaphoreBulkhead;
import com.xjtu.iron.executor.DefaultGovernanceExecutor;
import com.xjtu.iron.governor.BulkheadGovernor;
import org.springframework.context.annotation.Bean;

import java.util.List;

public class GovernanceConfig {
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = new BulkheadRegistry();
        registry.register(TagRule.builder().api("order.create").build(), new SemaphoreBulkhead(50));
        registry.register(TagRule.builder().api("order.create").tenant("vip").build(), new SemaphoreBulkhead(20));
        return registry;
    }

    @Bean
    public ConcurrencyGovernor concurrencyGovernor(BulkheadRegistry registry) {
        return new BulkheadGovernor(registry);
    }

    @Bean
    public GovernanceExecutor governanceExecutor(List<ConcurrencyGovernor> governorList) {
        return new DefaultGovernanceExecutor(governorList);
    }
}
