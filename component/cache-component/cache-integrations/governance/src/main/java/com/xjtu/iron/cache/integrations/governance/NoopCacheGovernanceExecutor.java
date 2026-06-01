package com.xjtu.iron.cache.integrations.governance;

import java.util.concurrent.Callable;

/**
 * 空治理执行器。
 *
 * <p>一期默认不做额外治理，直接执行 callable。</p>
 */
public class NoopCacheGovernanceExecutor implements CacheGovernanceExecutor {

    /** 直接执行目标调用。 */
    @Override
    public <T> T execute(String resourceName, Callable<T> callable) throws Exception {
        return callable.call();
    }
}
