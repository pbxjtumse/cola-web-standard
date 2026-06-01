package com.xjtu.iron.cache.integrations.governance;

import java.util.concurrent.Callable;

/**
 * 缓存治理执行器。
 *
 * <p>一期只预留接口。二期可以接入 governance-component、Sentinel 或 Resilience4j，
 * 对 Redis 调用、loader 调用做超时、熔断、限流和降级。</p>
 */
public interface CacheGovernanceExecutor {

    /** 在治理保护下执行指定调用。 */
    <T> T execute(String resourceName, Callable<T> callable) throws Exception;
}
