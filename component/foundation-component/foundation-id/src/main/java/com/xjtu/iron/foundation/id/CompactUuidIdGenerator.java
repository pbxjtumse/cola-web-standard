package com.xjtu.iron.foundation.id;

import java.util.UUID;

/**
 * 生成不包含短横线的紧凑 UUID。
 */
public final class CompactUuidIdGenerator implements StringIdGenerator {

    public static final CompactUuidIdGenerator INSTANCE = new CompactUuidIdGenerator();

    private CompactUuidIdGenerator() {
    }

    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
