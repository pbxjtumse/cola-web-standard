package com.xjtu.iron.cache.integrations.governance;

import java.util.concurrent.Callable;

public class NoopCacheGovernanceExecutor implements CacheGovernanceExecutor {

    @Override
    public <T> T execute(String resourceName, Callable<T> callable) throws Exception {
        return callable.call();
    }

}
