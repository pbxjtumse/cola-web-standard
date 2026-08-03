package com.xjtu.iron.foundation.serialization;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 定义单次序列化调用的通用选项。
 */
public final class SerializationOptions {

    /** 默认允许处理的最大载荷字节数。 */
    private static final int DEFAULT_MAX_BYTES = 16 * 1024 * 1024;
    /** 可复用的默认序列化选项。 */
    private static final SerializationOptions DEFAULTS = builder().build();

    /** 文本与字节转换使用的字符集。 */
    private final Charset charset;
    /** 单次调用允许处理的最大字节数。 */
    private final int maxBytes;
    /** 是否输出带缩进的可读文本。 */
    private final boolean prettyPrint;
    /** 反序列化遇到未知属性时是否失败。 */
    private final boolean failOnUnknownProperties;

    private SerializationOptions(Builder builder) {
        this.charset = builder.charset;
        this.maxBytes = builder.maxBytes;
        this.prettyPrint = builder.prettyPrint;
        this.failOnUnknownProperties = builder.failOnUnknownProperties;
    }

    public static SerializationOptions defaults() { return DEFAULTS; }
    public static Builder builder() { return new Builder(); }

    public Charset getCharset() { return charset; }
    public int getMaxBytes() { return maxBytes; }
    public boolean isPrettyPrint() { return prettyPrint; }
    public boolean isFailOnUnknownProperties() { return failOnUnknownProperties; }

    public static final class Builder {
        /** 文本与字节转换使用的字符集。 */
        private Charset charset = StandardCharsets.UTF_8;
        /** 单次调用允许处理的最大字节数。 */
        private int maxBytes = DEFAULT_MAX_BYTES;
        /** 是否输出带缩进的可读文本。 */
        private boolean prettyPrint;
        /** 反序列化遇到未知属性时是否失败。 */
        private boolean failOnUnknownProperties;

        public Builder charset(Charset charset) {
            this.charset = java.util.Objects.requireNonNull(charset, "charset must not be null");
            return this;
        }

        public Builder maxBytes(int maxBytes) {
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be positive");
            }
            this.maxBytes = maxBytes;
            return this;
        }

        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public Builder failOnUnknownProperties(boolean value) {
            this.failOnUnknownProperties = value;
            return this;
        }

        public SerializationOptions build() {
            return new SerializationOptions(this);
        }
    }
}
