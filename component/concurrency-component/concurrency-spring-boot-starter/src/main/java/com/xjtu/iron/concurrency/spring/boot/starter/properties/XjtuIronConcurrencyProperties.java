package com.xjtu.iron.concurrency.spring.boot.starter.properties;

import com.xjtu.iron.concurrency.api.enums.QueueType;
import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XJTU Iron 并发组件配置属性。
 */
@ConfigurationProperties(prefix = "xjtu.iron.concurrency")
public class XjtuIronConcurrencyProperties {

    /**
     * 是否启用并发组件。
     */
    private boolean enabled = true;

    /**
     * 默认线程池名称。
     */
    private String defaultExecutor = "default";

    /**
     * 上下文传播配置。
     */
    private ContextProperties context = new ContextProperties();

    private Duration rejectionWaitTime = Duration.ofMillis(100);

    /**
     * 线程池配置集合。
     */
    private Map<String, ThreadPoolProperties> threadPools = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultExecutor() {
        return defaultExecutor;
    }

    public void setDefaultExecutor(String defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }

    public ContextProperties getContext() {
        return context;
    }

    public void setContext(ContextProperties context) {
        this.context = context;
    }

    public Map<String, ThreadPoolProperties> getThreadPools() {
        return threadPools;
    }

    public void setThreadPools(Map<String, ThreadPoolProperties> threadPools) {
        this.threadPools = threadPools;
    }

    public Duration getRejectionWaitTime() {
        return rejectionWaitTime;
    }

    public void setRejectionWaitTime(Duration rejectionWaitTime) {
        this.rejectionWaitTime = rejectionWaitTime;
    }

    /**
     * 上下文传播配置。
     */
    public static class ContextProperties {

        /**
         * 是否启用上下文传播。
         */
        private boolean enabled = true;

        /**
         * 是否启用 MDC 传播。
         */
        private boolean mdcEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isMdcEnabled() {
            return mdcEnabled;
        }

        public void setMdcEnabled(boolean mdcEnabled) {
            this.mdcEnabled = mdcEnabled;
        }
    }

    /**
     * 单个线程池配置。
     */
    public static class ThreadPoolProperties {

        /**
         * 核心线程数。
         */
        private int corePoolSize = 4;

        /**
         * 最大线程数。
         */
        private int maxPoolSize = 16;

        /**
         * 队列容量。
         */
        private int queueCapacity = 1000;

        /**
         * 空闲线程存活时间。
         */
        private Duration keepAliveTime = Duration.ofSeconds(60);

        /**
         * 是否允许核心线程超时。
         */
        private boolean allowCoreThreadTimeout = false;

        /**
         * 线程名前缀。
         */
        private String threadNamePrefix;

        /**
         * 工作队列类型。
         */
        private QueueType queueType = QueueType.BOUNDED_LINKED_BLOCKING_QUEUE;

        /**
         * 拒绝策略。
         */
        private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;

        /**
         * BLOCKING_WAIT 拒绝策略的最大等待时间。
         */
        private Duration rejectionWaitTime = Duration.ofMillis(100);

        /**
         * 应用关闭时是否等待任务完成。
         */
        private boolean waitForTasksToCompleteOnShutdown = true;

        /**
         * 应用关闭时等待线程池终止的最大时间。
         */
        private Duration awaitTermination = Duration.ofSeconds(30);

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

        public Duration getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(Duration keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public boolean isAllowCoreThreadTimeout() {
            return allowCoreThreadTimeout;
        }

        public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public QueueType getQueueType() {
            return queueType;
        }

        public void setQueueType(QueueType queueType) {
            this.queueType = queueType;
        }

        public RejectionPolicy getRejectionPolicy() {
            return rejectionPolicy;
        }

        public void setRejectionPolicy(RejectionPolicy rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
        }

        public Duration getRejectionWaitTime() {
            return rejectionWaitTime;
        }

        public void setRejectionWaitTime(Duration rejectionWaitTime) {
            this.rejectionWaitTime = rejectionWaitTime;
        }

        public boolean isWaitForTasksToCompleteOnShutdown() {
            return waitForTasksToCompleteOnShutdown;
        }

        public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
            this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
        }

        public Duration getAwaitTermination() {
            return awaitTermination;
        }

        public void setAwaitTermination(Duration awaitTermination) {
            this.awaitTermination = awaitTermination;
        }
    }
}