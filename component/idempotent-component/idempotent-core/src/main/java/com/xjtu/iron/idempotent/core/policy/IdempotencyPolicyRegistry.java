package com.xjtu.iron.idempotent.core.policy;

import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 把 policyName / inline policy 解析成一次执行使用的稳定策略。
 *
 * <p>解析优先级固定为：</p>
 * <pre>
 * inline policy
 *   &gt; policyName
 *   &gt; default policy
 * </pre>
 */
public interface IdempotencyPolicyRegistry {

    IdempotencyPolicy resolve(String policyName, IdempotencyPolicy inlinePolicy);
}
