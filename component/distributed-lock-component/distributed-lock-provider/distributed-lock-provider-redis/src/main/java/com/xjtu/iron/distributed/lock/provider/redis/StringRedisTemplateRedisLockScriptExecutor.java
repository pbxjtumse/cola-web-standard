package com.xjtu.iron.distributed.lock.provider.redis;

import java.util.List;

public class StringRedisTemplateRedisLockScriptExecutor implements RedisLockScriptExecutor {
    @Override
    public Object execute(String scriptLocation, List<String> keys, List<String> args) {
        return null;
    }
}
