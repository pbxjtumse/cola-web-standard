package com.xjtu.iron.concurrency.api.execution.pool;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;

import java.time.Duration;

/**
 * 线程池运行时更新请求。
 *
 * <p>一期支持更新 core、max、keepAlive、allowCoreThreadTimeout、拒绝策略。
 * 队列类型和队列容量不支持原地修改。</p>
 */
public class ThreadPoolUpdateRequest {

    /** 新核心线程数。 */
    private Integer corePoolSize;

    /** 新最大线程数。 */
    private Integer maximumPoolSize;

    /** 新空闲线程存活时间。 */
    private Duration keepAliveTime;

    /** 是否允许核心线程超时。 */
    private Boolean allowCoreThreadTimeout;

    /** 是否预启动所有核心线程。 */
    private Boolean prestartAllCoreThreads;

    /** 新拒绝策略。 */
    private RejectionPolicy rejectionPolicy;

    /** BLOCKING_WAIT 策略的等待时间。 */
    private Duration rejectionWaitTime;

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Boolean getAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    public void setAllowCoreThreadTimeout(Boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    public Boolean getPrestartAllCoreThreads() {
        return prestartAllCoreThreads;
    }

    public void setPrestartAllCoreThreads(Boolean prestartAllCoreThreads) {
        this.prestartAllCoreThreads = prestartAllCoreThreads;
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
}
