package com.xjtu.iron.config;


import com.xjtu.iron.cola.web.config.ThreadPoolConfig;
import com.xjtu.iron.cola.web.enums.RejectedPolicyEnum;
import com.xjtu.iron.cola.web.factory.ThreadPoolFactory;
import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorBootstrap {

    @Autowired
    private  ExecutorProperties properties;

    @Autowired
    private  ThreadPoolRegistry registry;

    @PostConstruct
    public void init() {
        for (Map.Entry<String, ExecutorProperties.ExecutorItem> e : properties.getItems().entrySet()) {
            String name = e.getKey();
            ExecutorProperties.ExecutorItem item = e.getValue();
            ThreadPoolConfig config = ThreadPoolConfig.builder()
                    .name(name)
                    .coreSize(item.getCoreSize())
                    .maxSize(item.getMaxSize())
                    .queueSize(item.getQueueSize())
                    .rejectPolicy(RejectedPolicyEnum.valueOf(item.getRejectPolicy().name()).getCode())
                    .build();

            ExecutorService executor = ThreadPoolFactory.create(config);
            registry.register(
                    name,
                    executor,
                    new HashSet<>(item.getTags())
            );
        }
    }
}

