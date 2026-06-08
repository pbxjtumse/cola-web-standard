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

    /** 是否启用并发组件。 */
    private boolean enabled = true;

    /** 默认线程池名称。 */
    private String defaultExecutor = "default";

    /** 上下文传播配置。 */
    private ContextProperties context = new ContextProperties();

    /** 诊断配置。 */
    private DiagnosticsProperties diagnostics = new DiagnosticsProperties();

    /** 管理接口配置。 */
    private ManagementProperties management = new ManagementProperties();

    /** 任务状态注册表配置。 */
    private TaskRegistryProperties taskRegistry = new TaskRegistryProperties();

    /** 线程池配置集合。 */
    private Map<String, ThreadPoolProperties> threadPools = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDefaultExecutor() { return defaultExecutor; }
    public void setDefaultExecutor(String defaultExecutor) { this.defaultExecutor = defaultExecutor; }
    public ContextProperties getContext() { return context; }
    public void setContext(ContextProperties context) { this.context = context; }
    public DiagnosticsProperties getDiagnostics() { return diagnostics; }
    public void setDiagnostics(DiagnosticsProperties diagnostics) { this.diagnostics = diagnostics; }
    public ManagementProperties getManagement() { return management; }
    public void setManagement(ManagementProperties management) { this.management = management; }
    public TaskRegistryProperties getTaskRegistry() { return taskRegistry; }
    public void setTaskRegistry(TaskRegistryProperties taskRegistry) { this.taskRegistry = taskRegistry; }
    public Map<String, ThreadPoolProperties> getThreadPools() { return threadPools; }
    public void setThreadPools(Map<String, ThreadPoolProperties> threadPools) { this.threadPools = threadPools; }

    /** 上下文传播配置。 */
    public static class ContextProperties {
        private boolean enabled = true;
        private boolean mdcEnabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isMdcEnabled() { return mdcEnabled; }
        public void setMdcEnabled(boolean mdcEnabled) { this.mdcEnabled = mdcEnabled; }
    }

    /** 线程池诊断配置。 */
    public static class DiagnosticsProperties {
        private boolean enabled = true;
        private double activeUsageWarnThreshold = 0.9D;
        private double queueUsageWarnThreshold = 0.8D;
        private boolean downWhenBusy = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getActiveUsageWarnThreshold() { return activeUsageWarnThreshold; }
        public void setActiveUsageWarnThreshold(double activeUsageWarnThreshold) { this.activeUsageWarnThreshold = activeUsageWarnThreshold; }
        public double getQueueUsageWarnThreshold() { return queueUsageWarnThreshold; }
        public void setQueueUsageWarnThreshold(double queueUsageWarnThreshold) { this.queueUsageWarnThreshold = queueUsageWarnThreshold; }
        public boolean isDownWhenBusy() { return downWhenBusy; }
        public void setDownWhenBusy(boolean downWhenBusy) { this.downWhenBusy = downWhenBusy; }
    }

    /** 只读管理接口配置。 */
    public static class ManagementProperties {
        private boolean enabled = true;
        private String basePath = "/iron-concurrency";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
    }

    /** 任务状态注册表配置。 */
    public static class TaskRegistryProperties {
        private int maxSize = 10000;
        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }

    /** 单个线程池配置。 */
    public static class ThreadPoolProperties {
        private int corePoolSize = 4;
        private int maxPoolSize = 16;
        private int queueCapacity = 1000;
        private Duration keepAliveTime = Duration.ofSeconds(60);
        private boolean allowCoreThreadTimeout = false;
        private String threadNamePrefix;
        private QueueType queueType = QueueType.BOUNDED_LINKED_BLOCKING_QUEUE;
        private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;
        private Duration rejectionWaitTime = Duration.ofMillis(100);
        private boolean waitForTasksToCompleteOnShutdown = true;
        private Duration awaitTermination = Duration.ofSeconds(30);
        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public Duration getKeepAliveTime() { return keepAliveTime; }
        public void setKeepAliveTime(Duration keepAliveTime) { this.keepAliveTime = keepAliveTime; }
        public boolean isAllowCoreThreadTimeout() { return allowCoreThreadTimeout; }
        public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) { this.allowCoreThreadTimeout = allowCoreThreadTimeout; }
        public String getThreadNamePrefix() { return threadNamePrefix; }
        public void setThreadNamePrefix(String threadNamePrefix) { this.threadNamePrefix = threadNamePrefix; }
        public QueueType getQueueType() { return queueType; }
        public void setQueueType(QueueType queueType) { this.queueType = queueType; }
        public RejectionPolicy getRejectionPolicy() { return rejectionPolicy; }
        public void setRejectionPolicy(RejectionPolicy rejectionPolicy) { this.rejectionPolicy = rejectionPolicy; }
        public Duration getRejectionWaitTime() { return rejectionWaitTime; }
        public void setRejectionWaitTime(Duration rejectionWaitTime) { this.rejectionWaitTime = rejectionWaitTime; }
        public boolean isWaitForTasksToCompleteOnShutdown() { return waitForTasksToCompleteOnShutdown; }
        public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) { this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown; }
        public Duration getAwaitTermination() { return awaitTermination; }
        public void setAwaitTermination(Duration awaitTermination) { this.awaitTermination = awaitTermination; }
    }
}
