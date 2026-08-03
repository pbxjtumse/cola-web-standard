package com.xjtu.iron.foundation.id;

import java.util.UUID;

/**
 * 生成标准带短横线 UUID。
 */
public final class UuidIdGenerator implements StringIdGenerator {

    public static final UuidIdGenerator INSTANCE = new UuidIdGenerator();

    private UuidIdGenerator() {
    }

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
