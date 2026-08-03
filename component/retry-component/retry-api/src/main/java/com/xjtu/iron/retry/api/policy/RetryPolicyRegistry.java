package com.xjtu.iron.retry.api.policy;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** 管理命名重试策略的注册、替换和查询。 */
public interface RetryPolicyRegistry {

    /** 注册新策略；同名策略已存在时必须失败。 */
    void register(RetryPolicy retryPolicy);

    /** 显式替换同名策略；原策略不存在时也允许新增。 */
    void replace(RetryPolicy retryPolicy);

    /** 查找可选命名策略。 */
    Optional<RetryPolicy> find(String policyName);

    RetryPolicy getRequired(String policyName);

    /** 返回按名称排序的不可变策略名集合。 */
    Collection<String> policyNames();

    /** 返回按名称排序的不可变策略快照。 */
    Map<String, RetryPolicy> snapshot();
}
