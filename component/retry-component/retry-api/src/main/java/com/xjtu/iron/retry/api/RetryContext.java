package com.xjtu.iron.retry.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 一次逻辑重试过程在某次尝试时的不可变上下文快照。
 */
public final class RetryContext {

    /**
     * 当前逻辑重试过程的唯一标识。
     */
    private final String retryId;

    /**
     * 当前业务操作名称。
     */
    private final String operationName;

    /**
     * 当前使用的策略名称。
     */
    private final String policyName;

    /**
     * 当前尝试序号，从 1 开始。
     */
    private final int attempt;

    /**
     * 逻辑重试过程开始时间。
     */
    private final Instant startTime;

    /**
     * 创建当前快照时已经经过的时间。
     */
    private final Duration elapsedTime;

    /**
     * 上一次或当前一次尝试返回值。
     */
    private final Object lastResult;

    /**
     * 上一次或当前一次尝试异常。
     */
    private final Throwable lastFailure;

    /**
     * 调用方附加的只读属性。
     */
    private final Map<String, Object> attributes;

    public RetryContext(
            String retryId,
            String operationName,
            String policyName,
            int attempt,
            Instant startTime,
            Duration elapsedTime,
            Object lastResult,
            Throwable lastFailure,
            Map<String, Object> attributes) {
        this.retryId = requireText(retryId, "retryId");
        this.operationName = requireText(operationName, "operationName");
        this.policyName = requireText(policyName, "policyName");
        if (attempt < 1) {
            throw new IllegalArgumentException("attempt must be greater than or equal to 1");
        }
        this.attempt = attempt;
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.elapsedTime = requireNonNegative(elapsedTime, "elapsedTime");
        this.lastResult = lastResult;
        this.lastFailure = lastFailure;
        Map<String, Object> actualAttributes = attributes == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(attributes);
        this.attributes = Collections.unmodifiableMap(actualAttributes);
    }

    /**
     * 基于当前上下文创建包含本次尝试结果的新快照。
     */
    public RetryContext withOutcome(Duration elapsedTime, Object result, Throwable failure) {
        return new RetryContext(
                retryId,
                operationName,
                policyName,
                attempt,
                startTime,
                elapsedTime,
                result,
                failure,
                attributes
        );
    }

    public String getRetryId() {
        return retryId;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public int getAttempt() {
        return attempt;
    }

    public boolean isFirstAttempt() {
        return attempt == 1;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }

    public Object getLastResult() {
        return lastResult;
    }

    public Throwable getLastFailure() {
        return lastFailure;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static Duration requireNonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }
}
