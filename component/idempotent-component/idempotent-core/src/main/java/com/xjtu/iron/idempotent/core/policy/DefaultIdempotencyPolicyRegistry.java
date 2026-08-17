package com.xjtu.iron.idempotent.core.policy;

import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认命名策略注册表。
 *
 * <p>解析优先级固定为：</p>
 * <pre>
 * inline policy
 *   > legacy options
 *   > policyName
 *   > default policy
 * </pre>
 */
public final class DefaultIdempotencyPolicyRegistry implements IdempotencyPolicyRegistry {

    private final Map<String, IdempotencyPolicy> policies = new LinkedHashMap<>();
    private final String defaultPolicyName;

    public DefaultIdempotencyPolicyRegistry(
            List<IdempotencyPolicy> policyList,
            String defaultPolicyName) {
        if (policyList != null) {
            for (IdempotencyPolicy policy : policyList) {
                Objects.requireNonNull(policy, "policy must not be null");
                String name = policy.getName();
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException(
                            "registered IdempotencyPolicy requires a non-blank name");
                }
                IdempotencyPolicy old = policies.put(name, policy);
                if (old != null) {
                    throw new IllegalArgumentException("duplicate idempotency policy: " + name);
                }
            }
        }
        this.defaultPolicyName = normalize(defaultPolicyName);
        if (this.defaultPolicyName != null
                && !policies.containsKey(this.defaultPolicyName)) {
            throw new IllegalArgumentException(
                    "default idempotency policy not found: " + this.defaultPolicyName);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public IdempotencyPolicy resolve(
            String policyName,
            IdempotencyPolicy inlinePolicy,
            IdempotencyOptions legacyOptions) {

        if (inlinePolicy != null) {
            inlinePolicy.validate();
            return inlinePolicy;
        }
        if (legacyOptions != null) {
            IdempotencyPolicy policy = legacyOptions.toPolicy();
            policy.validate();
            return policy;
        }

        String resolvedName = normalize(policyName);
        if (resolvedName == null) {
            resolvedName = defaultPolicyName;
        }
        if (resolvedName == null) {
            throw new IllegalArgumentException(
                    "no idempotency policy selected and no default policy configured");
        }

        IdempotencyPolicy policy = policies.get(resolvedName);
        if (policy == null) {
            throw new IllegalArgumentException("idempotency policy not found: " + resolvedName);
        }
        policy.validate();
        return policy;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
