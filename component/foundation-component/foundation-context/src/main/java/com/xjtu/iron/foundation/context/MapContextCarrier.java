package com.xjtu.iron.foundation.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 Map 的上下文载体实现。
 */
public final class MapContextCarrier implements ContextCarrier {

    /** 载体中保存的字符串上下文条目。 */
    private final Map<String, String> values = new LinkedHashMap<>();

    public MapContextCarrier() {
    }

    public MapContextCarrier(Map<String, String> initialValues) {
        if (initialValues != null) {
            values.putAll(initialValues);
        }
    }

    @Override
    public void put(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("context carrier name must not be blank");
        }
        if (value == null) {
            values.remove(name);
        } else {
            values.put(name, value);
        }
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
