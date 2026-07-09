package com.xjtu.iron.distributed.lock.provider.redis;

import java.util.List;

/**
 * Redis Lua 脚本执行器。
 *
 * <p>该接口用于隔离 Redis 客户端实现。后续可以用 StringRedisTemplate、RedisTemplate、Lettuce、Jedis 或 Redisson
 * 实现本接口，而 RedisLockProvider 不需要关心具体客户端细节。</p>
 */
public interface RedisLockScriptExecutor {

    /**
     * 执行 Lua 脚本。
     *
     * @param script Lua 脚本。
     * @param keys   KEYS 参数。
     * @param args   ARGV 参数。
     * @return Redis 返回值。
     */
    Object execute(String script, List<String> keys, List<String> args);
}
