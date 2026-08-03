package com.xjtu.iron.foundation.test.id;

import com.xjtu.iron.foundation.id.api.IdGenerator;

/**
 * 始终返回固定标识的测试生成器。
 */
public final class FixedStringIdGenerator implements IdGenerator<String> {

    /** 每次调用都返回的固定测试标识。 */
    private final String value;

    public FixedStringIdGenerator(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        this.value = value;
    }

    @Override
    public String nextId() {
        return value;
    }
}
