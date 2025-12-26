package com.xjtu.iron.config;

import com.xjtu.iron.cola.web.enums.RejectedPolicyEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "executors")
public class ExecutorProperties {
    private Map<String, ExecutorItem> items = new HashMap<>();
    @Getter
    @Setter
    public static class ExecutorItem {
        private String name;
        private int coreSize;
        private int maxSize;
        private long keepAliveSeconds;
        private int queueSize;
        private RejectedPolicyEnum rejectPolicy;
        private Set<String> tags;

        // 构造方法
        public ExecutorItem() {}

        public ExecutorItem(String name, int coreSize, int maxSize,
                           int queueSize, RejectedPolicyEnum rejectPolicy, Set<String> tags) {
            this.name = name;
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueSize = queueSize;
            this.rejectPolicy = rejectPolicy;
            this.tags = tags;
        }

        // Additional convenience constructor without keepAliveSeconds
        public ExecutorItem(String name, int coreSize, int maxSize,
                           int queueSize, RejectedPolicyEnum rejectPolicy, Set<String> tags, long keepAliveSeconds) {
            this.name = name;
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueSize = queueSize;
            this.rejectPolicy = rejectPolicy;
            this.tags = tags;
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }
}

