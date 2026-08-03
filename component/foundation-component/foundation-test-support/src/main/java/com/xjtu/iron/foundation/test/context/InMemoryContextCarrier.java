package com.xjtu.iron.foundation.test.context;

import com.xjtu.iron.foundation.context.ContextCarrier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于上下文传播测试的内存载体。
 */
public final class InMemoryContextCarrier implements ContextCarrier {

    /** 测试载体中保存的上下文条目。 */
    private final Map<String, String> values = new LinkedHashMap<>();

    @Override
    public void put(String name, String value) {
        values.put(name, value);
    }

    @Override
    public String get(String name) {
        return values.get(name);
    }

    @Override
    public Map<String, String> entries() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
