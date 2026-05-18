package com.xjtu.iron.cache.integrations.governance;

import java.util.concurrent.Callable;

public interface CacheGovernanceExecutor {

    <T> T execute(String resourceName, Callable<T> callable) throws Exception;
}
