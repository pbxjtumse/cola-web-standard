package com.xjtu.iron.distributed.lock.provider.redis;

/**
 * Redis 分布式锁 Lua 脚本。
 *
 * <p>一期 Redis Provider 必须保证加锁、解锁、续期、检查这些关键操作具备原子性。尤其解锁和续期必须校验
 * ownerToken，不能直接 DEL 或 PEXPIRE。</p>
 */
public final class RedisLockScripts {

    public static final String ACQUIRE = "META-INF/iron-lock/redis/acquire.lua";

    public static final String RELEASE = "META-INF/iron-lock/redis/release.lua";

    public static final String RENEW = "META-INF/iron-lock/redis/renew.lua";

    public static final String CHECK = "META-INF/iron-lock/redis/check.lua";

    private RedisLockScripts() {
    }
}
