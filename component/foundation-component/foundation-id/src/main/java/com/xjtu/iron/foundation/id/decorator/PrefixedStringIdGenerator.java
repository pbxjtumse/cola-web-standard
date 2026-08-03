package com.xjtu.iron.foundation.id.decorator;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.util.Objects;

/** 在通用标识前添加稳定业务无关前缀。 */
public final class PrefixedStringIdGenerator implements StringIdGenerator {

    private final String prefix;
    private final String separator;
    private final StringIdGenerator delegate;

    public PrefixedStringIdGenerator(String prefix, StringIdGenerator delegate) {
        this(prefix, "-", delegate);
    }

    public PrefixedStringIdGenerator(
            String prefix,
            String separator,
            StringIdGenerator delegate) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
        this.separator = Objects.requireNonNull(separator, "separator must not be null");
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public String nextId() {
        return prefix + separator + delegate.nextId();
    }
}
