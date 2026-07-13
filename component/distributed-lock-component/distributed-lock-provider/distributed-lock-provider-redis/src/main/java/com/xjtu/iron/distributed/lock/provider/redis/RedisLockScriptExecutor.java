package com.xjtu.iron.distributed.lock.provider.redis;

import java.util.List;

/**
 * Redis Lua 脚本执行器。
 *
 * <p>该接口用于隔离 Redis 客户端实现。后续可以用 StringRedisTemplate、RedisTemplate、Lettuce、Jedis
 * 或 Redisson 实现本接口，而 {@link RedisLockProvider} 不需要关心具体 Redis 客户端细节。</p>
 *
 * <p>参数 scriptLocation 使用 {@link RedisLockScripts} 中定义的 classpath 资源路径。具体实现可以选择：
 * 每次读取脚本执行，也可以启动时加载脚本并缓存 SHA，再通过 EVALSHA 执行。</p>
 */
public interface RedisLockScriptExecutor {

    /**
     * 执行 Lua 脚本。
     *
     * @param scriptLocation Lua 脚本 classpath 资源路径。
     * @param keys           Redis KEYS 参数。
     * @param args           Redis ARGV 参数。
     * @return Redis 返回值。
     */
    Object execute(String scriptLocation, List<String> keys, List<String> args);
}
