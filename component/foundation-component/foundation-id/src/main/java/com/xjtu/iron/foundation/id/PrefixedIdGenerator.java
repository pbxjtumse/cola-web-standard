package com.xjtu.iron.foundation.id;

import java.util.Objects;

/**
 * 为另一个字符串标识生成器增加固定前缀。
 */
public final class PrefixedIdGenerator implements StringIdGenerator {

    /** 每个生成标识固定追加的前缀。 */
    private final String prefix;
    /** 实际生成主体标识的委托生成器。 */
    private final StringIdGenerator delegate;

    public PrefixedIdGenerator(String prefix, StringIdGenerator delegate) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public String nextId() {
        return prefix + delegate.nextId();
    }
}
