package com.xjtu.iron.retry.api;

import java.util.Optional;

/**
 * 命名重试策略注册表。
 */
public interface RetryPolicyRegistry {

    /**
     * 注册或替换一个命名策略。
     */
    void register(RetryPolicy retryPolicy);

    /**
     * 查找命名策略。
     */
    Optional<RetryPolicy> find(String policyName);

    /**
     * 获取必需策略，不存在时抛出异常。
     */
    RetryPolicy getRequired(String policyName);
}
