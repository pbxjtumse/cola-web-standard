package com.xjtu.iron.foundation.id.registry;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 按名称管理不同用途的字符串标识生成器。
 */
public final class IdGeneratorRegistry {

    /** 按名称注册的不可变标识生成器集合。 */
    private final Map<String, StringIdGenerator> generators;

    public IdGeneratorRegistry(Map<String, ? extends StringIdGenerator> generators) {
        if (generators == null || generators.isEmpty()) {
            throw new IllegalArgumentException("generators must not be empty");
        }
        LinkedHashMap<String, StringIdGenerator> copy = new LinkedHashMap<>();
        generators.forEach((name, generator) -> {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("generator name must not be blank");
            }
            copy.put(name, Objects.requireNonNull(generator, "generator must not be null"));
        });
        this.generators = Collections.unmodifiableMap(copy);
    }

    /**
     * 获取指定生成器；不存在时立即失败，避免静默退回错误策略。
     */
    public StringIdGenerator require(String name) {
        StringIdGenerator generator = generators.get(name);
        if (generator == null) {
            throw new IllegalArgumentException("unknown id generator: " + name);
        }
        return generator;
    }

    public Map<String, StringIdGenerator> getGenerators() {
        return generators;
    }
}
