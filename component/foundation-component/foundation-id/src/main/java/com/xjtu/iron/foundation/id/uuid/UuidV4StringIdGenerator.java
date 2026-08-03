package com.xjtu.iron.foundation.id.uuid;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.util.UUID;

/** 使用 JDK 加密强随机源生成标准 UUID v4 文本。 */
public final class UuidV4StringIdGenerator implements StringIdGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
