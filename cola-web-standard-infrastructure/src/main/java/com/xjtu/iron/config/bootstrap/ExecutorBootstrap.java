package com.xjtu.iron.config.bootstrap;


import com.xjtu.iron.cola.web.config.ThreadPoolConfig;
import com.xjtu.iron.cola.web.factory.ThreadPoolFactory;
import com.xjtu.iron.cola.web.metric.ThreadPoolMetricsBinder;
import com.xjtu.iron.cola.web.registry.ThreadPoolRegistry;
import com.xjtu.iron.config.properties.ExecutorProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorBootstrap {

    @Autowired
    private ExecutorProperties properties;

    @Autowired
    private ThreadPoolRegistry registry;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        for (Map.Entry<String, ExecutorProperties.ExecutorItem> e : properties.getItems().entrySet()) {
            //遍历参数
            String name = e.getKey();
            ExecutorProperties.ExecutorItem item = e.getValue();
            ThreadPoolConfig config = ThreadPoolConfig.builder()
                    .name(name)
                    .coreSize(item.getCoreSize())
                    .maxSize(item.getMaxSize())
                    .queueSize(item.getQueueSize())
                    .keepAliveSeconds(item.getKeepAliveSeconds())
                    .rejectPolicy(item.getRejectedPolicy().getCode())
                    .build();
            //构造线程池
            ExecutorService executor = ThreadPoolFactory.create(config);
            // 注册线程池
            registry.register(name, executor, new HashSet<>(item.getTags()));
            // 绑定 Prometheus
            ThreadPoolMetricsBinder.bind(meterRegistry, name, (ThreadPoolExecutor) executor);
        }
    }
}


