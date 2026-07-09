package com.xjtu.iron.distributed.lock.core.name;

/**
 * 默认锁名称校验器。
 */
public final class DefaultLockNameValidator implements LockNameValidator {

    /** 默认最大锁名长度。 */
    private static final int DEFAULT_MAX_LENGTH = 512;

    /** 最大锁名长度。 */
    private final int maxLength;

    public DefaultLockNameValidator() {
        this(DEFAULT_MAX_LENGTH);
    }

    public DefaultLockNameValidator(int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        this.maxLength = maxLength;
    }

    @Override
    public void validate(String lockName) {
        if (lockName == null || lockName.trim().isEmpty()) {
            throw new IllegalArgumentException("lockName must not be blank");
        }
        if (lockName.length() > maxLength) {
            throw new IllegalArgumentException("lockName length must be less than or equal to " + maxLength);
        }
    }
}
