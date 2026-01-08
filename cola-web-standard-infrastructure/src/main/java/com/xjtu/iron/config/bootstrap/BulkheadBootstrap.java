package com.xjtu.iron.config.bootstrap;

import com.xjtu.iron.cola.web.Bulkhead;
import com.xjtu.iron.cola.web.dto.TagRule;
import com.xjtu.iron.cola.web.impl.bulk.BulkheadRegistry;
import com.xjtu.iron.cola.web.impl.bulk.semaphore.SemaphoreBulkhead;
import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import com.xjtu.iron.config.properties.BulkheadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(BulkheadProperties.class)
public class BulkheadBootstrap {

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    private BulkheadProperties properties;

    @PostConstruct
    public void init() {
        for (BulkheadProperties.Item item : properties.getItems()) {
            TagRule.Builder ruleBuilder = TagRule.builder();
            item.getRule().forEach(ruleBuilder::put);
            Bulkhead bulkhead = new SemaphoreBulkhead(item.getLimit());
            bulkheadRegistry.register(ruleBuilder.build(), bulkhead);
        }
    }
}

