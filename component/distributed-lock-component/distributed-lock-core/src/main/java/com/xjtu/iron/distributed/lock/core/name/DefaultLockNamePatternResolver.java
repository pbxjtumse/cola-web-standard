package com.xjtu.iron.distributed.lock.core.name;

/**
 * 默认锁名称归一化器。
 *
 * <p>默认策略比较保守：如果 lockName 超过两段，则保留前两段，后续统一替换为 {@code *}。
 * 例如 {@code settle:batch:20260708:001} 会变成 {@code settle:batch:*}。</p>
 */
public final class DefaultLockNamePatternResolver implements LockNamePatternResolver {

    @Override
    public String resolvePattern(String lockName) {
        if (lockName == null || lockName.trim().isEmpty()) {
            return "unknown";
        }
        String[] parts = lockName.split(":");
        if (parts.length <= 2) {
            return lockName;
        }
        return parts[0] + ':' + parts[1] + ":*";
    }
}
