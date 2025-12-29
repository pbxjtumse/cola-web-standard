package com.xjtu.iron.config;

import com.xjtu.iron.cola.web.enums.RejectedPolicyEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 声明“线程池长什么样”
 * @author pbxjt
 * @date 2025/12/29
 */
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
        private RejectedPolicyEnum rejectedPolicy;
        private Set<String> tags;

        // 构造方法
        public ExecutorItem() {}

        public ExecutorItem(String name, int coreSize, int maxSize,
                            int queueSize, RejectedPolicyEnum rejectedPolicy, Set<String> tags) {
            this.name = name;
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueSize = queueSize;
            this.rejectedPolicy = rejectedPolicy;
            this.tags = tags;
        }

        // Additional convenience constructor wit hout keepAliveSeconds
        public ExecutorItem(String name, int coreSize, int maxSize,
                            int queueSize, RejectedPolicyEnum rejectedPolicy, Set<String> tags, long keepAliveSeconds) {
            this.name = name;
            this.coreSize = coreSize;
            this.maxSize = maxSize;
            this.queueSize = queueSize;
            this.rejectedPolicy = rejectedPolicy;
            this.tags = tags;
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }
}

