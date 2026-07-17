package com.xjtu.iron.governance.starter.properties;

import com.xjtu.iron.governance.model.policy.GovernancePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "xjtu.iron.governance")
public class GovernanceProperties {

    private boolean enabled = true;

    private GovernancePolicy defaultPolicy = new GovernancePolicy();

    private Map<String, GovernancePolicy> resources = new HashMap<>();

    private TimeoutExecutorProperties timeoutExecutor = new TimeoutExecutorProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GovernancePolicy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void setDefaultPolicy(GovernancePolicy defaultPolicy) {
        this.defaultPolicy = defaultPolicy;
    }

    public Map<String, GovernancePolicy> getResources() {
        return resources;
    }

    public void setResources(Map<String, GovernancePolicy> resources) {
        this.resources = resources;
    }

    public TimeoutExecutorProperties getTimeoutExecutor() {
        return timeoutExecutor;
    }

    public void setTimeoutExecutor(TimeoutExecutorProperties timeoutExecutor) {
        this.timeoutExecutor = timeoutExecutor;
    }

    public static class TimeoutExecutorProperties {

        private int corePoolSize = 20;

        private int maxPoolSize = 100;

        private int queueCapacity = 1000;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }
}