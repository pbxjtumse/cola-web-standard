package com.xjtu.iron.idempotent.core.policy;

import com.xjtu.iron.idempotent.api.policy.IdempotencyOptions;
import com.xjtu.iron.idempotent.api.policy.IdempotencyPolicy;

/**
 * 把 policyName / inline policy / V1.2 options 解析成一次执行使用的稳定策略。
 */
public interface IdempotencyPolicyRegistry {

    IdempotencyPolicy resolve(
            String policyName,
            IdempotencyPolicy inlinePolicy,
            IdempotencyOptions legacyOptions);
}
