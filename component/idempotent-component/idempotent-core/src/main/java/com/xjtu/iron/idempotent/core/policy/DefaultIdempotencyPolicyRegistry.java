package com.xjtu.iron.idempotent.core.policy;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 默认命名 Policy 注册表。
 *
 * <p>它只解决“这一次请求最终使用哪一份稳定策略”，解析优先级固定为：</p>
 * <pre>
 * inline policy > policyName > default policy
 * </pre>
 *
 * <p>Request 负责描述“这一次是谁”，Policy 负责描述“这一类业务平时怎么做幂等”。</p>
 */
public final class DefaultIdempotencyPolicyRegistry implements IdempotencyPolicyRegistry {

    private final Map<String, IdempotencyPolicy> policies = new LinkedHashMap<>();
    private final String defaultPolicyName;

    public DefaultIdempotencyPolicyRegistry(List<IdempotencyPolicy> policyList, String defaultPolicyName) {
        if (policyList != null) {
            for (IdempotencyPolicy policy : policyList) {
                Objects.requireNonNull(policy, "policy must not be null");
                String name = policy.getName();
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("registered IdempotencyPolicy requires a non-blank name");
                }
                if (policies.put(name, policy) != null) {
                    throw new IllegalArgumentException("duplicate idempotency policy: " + name);
                }
            }
        }

        this.defaultPolicyName = normalize(defaultPolicyName);
        if (this.defaultPolicyName != null && !policies.containsKey(this.defaultPolicyName)) {
            throw new IllegalArgumentException("default idempotency policy not found: " + this.defaultPolicyName);
        }
    }

    /**
     * 解析本次请求使用的 Policy。
     *
     * <p>inline policy 适合少量调用级覆盖；日常业务更推荐 policyName，这样正常执行、Recovery 扫描、Repository 选择
     * 都复用同一份配置，不会因为多个地方手工拼参数产生策略漂移。</p>
     */
    @Override
    public IdempotencyPolicy resolve(String policyName, IdempotencyPolicy inlinePolicy) {
        if (inlinePolicy != null) {
            inlinePolicy.validate();
            return inlinePolicy;
        }

        String resolvedName = normalize(policyName);
        if (resolvedName == null) {
            resolvedName = defaultPolicyName;
        }
        if (resolvedName == null) {
            throw new IllegalArgumentException("no idempotency policy selected and no default policy configured");
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
