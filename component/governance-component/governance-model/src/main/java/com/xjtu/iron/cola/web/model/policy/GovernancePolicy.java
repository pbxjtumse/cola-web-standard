package com.xjtu.iron.cola.web.model.policy;

import com.xjtu.iron.cola.web.model.engine.GovernanceEngineType;
import com.xjtu.iron.cola.web.model.resource.GovernanceResourceType;

import java.util.HashMap;
import java.util.Map;

public class GovernancePolicy {

    private String resourceName;

    private GovernanceResourceType resourceType = GovernanceResourceType.OUTBOUND;

    private GovernanceEngineType preferredEngine = GovernanceEngineType.RESILIENCE4J;

    private TimeoutPolicy timeout = new TimeoutPolicy();

    private RetryPolicy retry = new RetryPolicy();

    private CircuitBreakerPolicy circuitBreaker = new CircuitBreakerPolicy();

    private BulkheadPolicy bulkhead = new BulkheadPolicy();

    private RateLimitPolicy rateLimit = new RateLimitPolicy();

    /**
     * 二期扩展字段，例如 Sentinel 特有配置、热点限流扩展等。
     */
    private Map<String, Object> extensions = new HashMap<>();

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public GovernanceResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(GovernanceResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public GovernanceEngineType getPreferredEngine() {
        return preferredEngine;
    }

    public void setPreferredEngine(GovernanceEngineType preferredEngine) {
        this.preferredEngine = preferredEngine;
    }

    public TimeoutPolicy getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutPolicy timeout) {
        this.timeout = timeout;
    }

    public RetryPolicy getRetry() {
        return retry;
    }

    public void setRetry(RetryPolicy retry) {
        this.retry = retry;
    }

    public CircuitBreakerPolicy getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerPolicy circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public BulkheadPolicy getBulkhead() {
        return bulkhead;
    }

    public void setBulkhead(BulkheadPolicy bulkhead) {
        this.bulkhead = bulkhead;
    }

    public RateLimitPolicy getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitPolicy rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }
}
