package com.xjtu.iron.concurrency.api.execution;

import com.xjtu.iron.concurrency.api.enums.RejectionPolicy;

import java.time.Duration;

/**
 * 线程池运行时更新请求。
 *
 * <p>只支持 ThreadPoolExecutor 原生可安全变更的能力。</p>
 * <p>队列类型、队列容量、线程工厂不在这里支持实时变更，通常需要重建线程池。</p>
 */
public class ThreadPoolUpdateRequest {

    /**
     * 新核心线程数。
     */
    private Integer corePoolSize;

    /**
     * 新最大线程数。
     */
    private Integer maximumPoolSize;

    /**
     * 新空闲线程存活时间。
     */
    private Duration keepAliveTime;

    /**
     * 是否允许核心线程超时回收。
     */
    private Boolean allowCoreThreadTimeout;

    /**
     * 新拒绝策略。
     */
    private RejectionPolicy rejectionPolicy;

    /**
     * 拒绝策略为 BLOCKING_WAIT 时的最大等待时间。
     */
    private Duration rejectionWaitTime;

    /**
     * 更新后是否预启动所有核心线程。
     */
    private Boolean prestartAllCoreThreads;

    /**
     * 创建只包含线程数调整的请求。
     *
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @return 更新请求
     */
    public static ThreadPoolUpdateRequest resize(int corePoolSize, int maximumPoolSize) {
        ThreadPoolUpdateRequest request = new ThreadPoolUpdateRequest();
        request.setCorePoolSize(corePoolSize);
        request.setMaximumPoolSize(maximumPoolSize);
        return request;
    }

    /**
     * 获取核心线程数。
     *
     * @return 核心线程数
     */
    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 设置核心线程数。
     *
     * @param corePoolSize 核心线程数
     */
    public void setCorePoolSize(Integer corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    /**
     * 获取最大线程数。
     *
     * @return 最大线程数
     */
    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置最大线程数。
     *
     * @param maximumPoolSize 最大线程数
     */
    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * 获取空闲线程存活时间。
     *
     * @return 空闲线程存活时间
     */
    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * 设置空闲线程存活时间。
     *
     * @param keepAliveTime 空闲线程存活时间
     */
    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * 获取是否允许核心线程超时。
     *
     * @return true 表示允许核心线程超时回收
     */
    public Boolean getAllowCoreThreadTimeout() {
        return allowCoreThreadTimeout;
    }

    /**
     * 设置是否允许核心线程超时。
     *
     * @param allowCoreThreadTimeout 是否允许核心线程超时
     */
    public void setAllowCoreThreadTimeout(Boolean allowCoreThreadTimeout) {
        this.allowCoreThreadTimeout = allowCoreThreadTimeout;
    }

    /**
     * 获取拒绝策略。
     *
     * @return 拒绝策略
     */
    public RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    /**
     * 设置拒绝策略。
     *
     * @param rejectionPolicy 拒绝策略
     */
    public void setRejectionPolicy(RejectionPolicy rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
    }

    /**
     * 获取拒绝等待时间。
     *
     * @return 拒绝等待时间
     */
    public Duration getRejectionWaitTime() {
        return rejectionWaitTime;
    }

    /**
     * 设置拒绝等待时间。
     *
     * @param rejectionWaitTime 拒绝等待时间
     */
    public void setRejectionWaitTime(Duration rejectionWaitTime) {
        this.rejectionWaitTime = rejectionWaitTime;
    }

    /**
     * 获取是否预启动核心线程。
     *
     * @return true 表示预启动所有核心线程
     */
    public Boolean getPrestartAllCoreThreads() {
        return prestartAllCoreThreads;
    }

    /**
     * 设置是否预启动核心线程。
     *
     * @param prestartAllCoreThreads 是否预启动核心线程
     */
    public void setPrestartAllCoreThreads(Boolean prestartAllCoreThreads) {
        this.prestartAllCoreThreads = prestartAllCoreThreads;
    }
}
