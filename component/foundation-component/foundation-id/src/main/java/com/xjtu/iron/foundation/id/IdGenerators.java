package com.xjtu.iron.foundation.id;

/**
 * 提供常用技术标识生成器工厂。
 */
public final class IdGenerators {

    private IdGenerators() {
    }

    public static StringIdGenerator uuid() {
        return UuidIdGenerator.INSTANCE;
    }

    public static StringIdGenerator compactUuid() {
        return CompactUuidIdGenerator.INSTANCE;
    }

    public static StringIdGenerator prefixed(String prefix) {
        return new PrefixedIdGenerator(prefix, compactUuid());
    }
}
