package com.xjtu.iron.cola.web.factory;

import com.xjtu.iron.cola.web.enums.RejectedPolicyEnum;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 第一层：Governor（主动拒绝）
 * 第二层：Executor Bulkhead（并发限制）
 * 第三层：ThreadPool RejectedPolicy（被动兜底）
 * @author pangbo
 * @date 2025/12/18
 */
public class RejectedPolicyFactory {
    /**
     * @param policy
     * @return {@link RejectedExecutionHandler }
     */
    public static RejectedExecutionHandler create(String policy) {
        if (policy == null) {
            throw new IllegalArgumentException("RejectedPolicyEnum cannot be null");
        }
        RejectedPolicyEnum rejectedPolicyEnum = RejectedPolicyEnum.getByCodeOrThrow(policy);
        switch (rejectedPolicyEnum) {
            case CALLER_RUNS:
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD:
                return new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST:
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            case ABORT:
            default:
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }
}
