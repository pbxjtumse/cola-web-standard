package com.xjtu.iron.distributed.lock.core.key;

import com.xjtu.iron.distributed.lock.api.LockOptions;

/**
 * 默认锁 key 构造器。
 */
public final class DefaultLockKeyBuilder implements LockKeyBuilder {

    /**
     * 组件 key 前缀。
     */
    private final String prefix;

    public DefaultLockKeyBuilder() {
        this("iron:lock");
    }

    public DefaultLockKeyBuilder(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = trimColon(prefix.trim());
    }

    @Override
    public String buildLockKey(String lockName, LockOptions options) {
        return prefix + ':' + normalize(options.getNamespace()) + ':' + normalize(lockName);
    }

    @Override
    public String buildFencingKey(String lockName, LockOptions options) {
        return buildLockKey(lockName, options) + ":fence";
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("lock key part must not be blank");
        }
        return trimColon(value.trim());
    }

    private static String trimColon(String value) {
        String result = value;
        while (result.startsWith(":")) {
            result = result.substring(1);
        }
        while (result.endsWith(":")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
