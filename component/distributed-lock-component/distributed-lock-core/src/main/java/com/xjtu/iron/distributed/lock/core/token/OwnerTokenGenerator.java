package com.xjtu.iron.distributed.lock.core.token;

/**
 * ownerToken 生成器。
 *
 * <p>ownerToken 是一次锁租约的唯一归属凭证。它应该具备足够随机性和可追踪性，避免不同进程、不同请求之间
 * 发生碰撞。</p>
 *
 * <p>注意：ownerToken 可以包含线程名用于排查问题，但不能把 Java Thread 作为 owner 语义。真正的 owner 是
 * 完整 token。</p>
 */
public interface OwnerTokenGenerator {

    /**
     * 生成新的 ownerToken。
     *
     * @param namespace 锁命名空间。
     * @param lockName  业务锁名称。
     * @return ownerToken。
     */
    String generate(String namespace, String lockName);
}
