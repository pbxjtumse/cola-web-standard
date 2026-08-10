package com.xjtu.iron.distributed.lock.provider.redisson;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * iron-lock ownerToken 与 Redisson owner threadId 的本地语义适配表。
 *
 * <p>为什么需要它：</p>
 * <ul>
 *     <li>iron-lock 把 ownerToken 定义为“一次成功 lease 的身份”，它不绑定 Java 线程；</li>
 *     <li>Redisson RLock/RFencedLock 的 owner 身份由 RedissonClientId + threadId 表示；</li>
 *     <li>因此 acquire 时记录原始 threadId，后续即使 LockHandle 被另一个 Java 线程释放，
 *         仍可调用 Redisson 的显式 threadId API 完成 owner-safe unlock/check。</li>
 * </ul>
 *
 * <p>另外，iron-lock 当前没有暴露“可重入”语义，因此同一个 Redisson owner threadId 对同一个 lockKey
 * 不能再次以新的 ownerToken 获取锁。这里用 {@code lockKey + threadId} 做本地占位，阻止 Redisson
 * 把第二次 acquire 当成 reentrant acquire。</p>
 *
 * <p>本表不是锁正确性的远端依据。真正的 owner 校验仍由 Redisson 的 Lua/锁实现完成；本表只保存
 * “哪个 Redisson threadId 对应哪个 iron ownerToken”的适配信息。</p>
 */
public final class RedissonOwnershipRegistry {

    private final ConcurrentMap<String, Ownership> byOwnerToken = new ConcurrentHashMap<>();
    private final ConcurrentMap<ThreadLockKey, String> activeOwners = new ConcurrentHashMap<>();

    /**
     * 为一次 acquire 预留 Redisson owner 身份。
     *
     * @return Reservation；reserved=false 表示同一个 threadId 已经持有/正在等待同一个 lockKey，
     *         当前 iron-lock 不允许把它解释成可重入成功。
     */
    public Reservation reserve(String ownerToken, String lockKey, long threadId) {
        String token = requireText(ownerToken, "ownerToken");
        String key = requireText(lockKey, "lockKey");
        ThreadLockKey threadLockKey = new ThreadLockKey(key, threadId);

        String conflictOwnerToken = activeOwners.putIfAbsent(threadLockKey, token);
        if (conflictOwnerToken != null) {
            return Reservation.conflict(conflictOwnerToken);
        }

        Ownership ownership = new Ownership(key, threadId);
        Ownership previous = byOwnerToken.putIfAbsent(token, ownership);
        if (previous != null) {
            activeOwners.remove(threadLockKey, token);
            throw new IllegalStateException("duplicate redisson ownerToken reservation: " + token);
        }
        return Reservation.reserved(ownership);
    }

    /** 查询 ownerToken 已绑定的 Redisson owner 信息。 */
    public Ownership find(String ownerToken) {
        if (ownerToken == null || ownerToken.trim().isEmpty()) {
            return null;
        }
        return byOwnerToken.get(ownerToken);
    }

    /**
     * 清理一个已经结束/明确失效的 owner 映射。
     * compare-remove 保证不会误删后来重新建立的其他 owner 记录。
     */
    public void remove(String ownerToken) {
        if (ownerToken == null) {
            return;
        }
        Ownership ownership = byOwnerToken.remove(ownerToken);
        if (ownership != null) {
            activeOwners.remove(new ThreadLockKey(ownership.lockKey(), ownership.threadId()), ownerToken);
        }
    }

    int size() {
        return byOwnerToken.size();
    }

    /** iron ownerToken 对应的 Redisson 锁名与 owner threadId。 */
    public static final class Ownership {
        private final String lockKey;
        private final long threadId;

        private Ownership(String lockKey, long threadId) {
            this.lockKey = lockKey;
            this.threadId = threadId;
        }

        public String lockKey() {
            return lockKey;
        }

        public long threadId() {
            return threadId;
        }
    }

    /** acquire 预留结果。 */
    public static final class Reservation {
        private final Ownership ownership;
        private final String conflictOwnerToken;

        private Reservation(Ownership ownership, String conflictOwnerToken) {
            this.ownership = ownership;
            this.conflictOwnerToken = conflictOwnerToken;
        }

        static Reservation reserved(Ownership ownership) {
            return new Reservation(Objects.requireNonNull(ownership), null);
        }

        static Reservation conflict(String conflictOwnerToken) {
            return new Reservation(null, conflictOwnerToken);
        }

        public boolean isReserved() {
            return ownership != null;
        }

        public Ownership ownership() {
            if (ownership == null) {
                throw new IllegalStateException("reservation is not successful");
            }
            return ownership;
        }

        public String conflictOwnerToken() {
            return conflictOwnerToken;
        }
    }

    private static final class ThreadLockKey {
        private final String lockKey;
        private final long threadId;

        private ThreadLockKey(String lockKey, long threadId) {
            this.lockKey = lockKey;
            this.threadId = threadId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ThreadLockKey)) {
                return false;
            }
            ThreadLockKey that = (ThreadLockKey) other;
            return threadId == that.threadId && lockKey.equals(that.lockKey);
        }

        @Override
        public int hashCode() {
            return 31 * lockKey.hashCode() + Long.hashCode(threadId);
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
