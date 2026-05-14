package com.xjtu.iron.governance.core.policy;

import com.xjtu.iron.governance.model.policy.*;
import lombok.Data;

@Data
public class GovernancePolicy {
    private String resourceName;
    private TimeoutPolicy timeout = new TimeoutPolicy();
    private RetryPolicy retry = new RetryPolicy();
    private CircuitBreakerPolicy circuitBreaker = new CircuitBreakerPolicy();
    private BulkheadPolicy bulkhead = new BulkheadPolicy();
    private RateLimitPolicy rateLimit = new RateLimitPolicy();
}