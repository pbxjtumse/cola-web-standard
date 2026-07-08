package com.xjtu.iron.distributed.lock.core.token;

/**
 * ownerToken 生成器。
 *
 * <p>ownerToken 是一次锁租约的唯一归属凭证。它应该具备足够随机性和可追踪性，避免不同进程、不同线程、
 * 不同请求之间发生碰撞。</p>
 *
 * <p>推荐组成：</p>
 * <pre>{@code
 * nodeId:processId:threadName:timestampNanos:uuid
 * }</pre>
 *
 * <p>注意：threadName 只用于排查问题，不用于判断锁 owner。真正的 owner 是完整 token。</p>
 */
public interface OwnerTokenGenerator {

    /**
     * 生成新的 ownerToken。
     *
     * @param lockName 业务锁名称。
     * @param lockKey  底层锁 key。
     * @return ownerToken。
     */
    String generate(String lockName, String lockKey);
}
