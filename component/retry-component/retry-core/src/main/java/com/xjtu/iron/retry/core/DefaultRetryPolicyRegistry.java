package com.xjtu.iron.retry.core;

import com.xjtu.iron.retry.api.RetryPolicy;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存并发映射表的默认命名策略注册表。
 */
public final class DefaultRetryPolicyRegistry implements RetryPolicyRegistry {

    private final Map<String, RetryPolicy> policies = new ConcurrentHashMap<>();

    @Override
    public void register(RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy must not be null");
        }
        policies.put(retryPolicy.getPolicyName(), retryPolicy);
    }

    @Override
    public Optional<RetryPolicy> find(String policyName) {
        return Optional.ofNullable(policies.get(requireText(policyName)));
    }

    @Override
    public RetryPolicy getRequired(String policyName) {
        String actualPolicyName = requireText(policyName);
        RetryPolicy retryPolicy = policies.get(actualPolicyName);
        if (retryPolicy == null) {
            throw new IllegalArgumentException("Retry policy does not exist: " + actualPolicyName);
        }
        return retryPolicy;
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("policyName must not be blank");
        }
        return value;
    }
}
