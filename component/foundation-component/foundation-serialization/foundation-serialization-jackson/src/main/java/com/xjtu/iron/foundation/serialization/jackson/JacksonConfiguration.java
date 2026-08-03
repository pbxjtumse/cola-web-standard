package com.xjtu.iron.foundation.serialization.jackson;

/**
 * 描述 Jackson 序列化器的稳定基础配置。
 */
public final class JacksonConfiguration {

    /** 全局是否拒绝未知 JSON 属性。 */
    private final boolean failOnUnknownProperties;
    /** 是否将日期时间输出为数字时间戳。 */
    private final boolean writeDatesAsTimestamps;
    /** 是否在 JSON 中保留值为 null 的属性。 */
    private final boolean includeNullValues;

    private JacksonConfiguration(Builder builder) {
        this.failOnUnknownProperties = builder.failOnUnknownProperties;
        this.writeDatesAsTimestamps = builder.writeDatesAsTimestamps;
        this.includeNullValues = builder.includeNullValues;
    }

    public static Builder builder() { return new Builder(); }
    public static JacksonConfiguration defaults() { return builder().build(); }

    public boolean isFailOnUnknownProperties() { return failOnUnknownProperties; }
    public boolean isWriteDatesAsTimestamps() { return writeDatesAsTimestamps; }
    public boolean isIncludeNullValues() { return includeNullValues; }

    public static final class Builder {
        /** 全局是否拒绝未知 JSON 属性。 */
        private boolean failOnUnknownProperties;
        /** 是否将日期时间输出为数字时间戳。 */
        private boolean writeDatesAsTimestamps;
        /** 是否在 JSON 中保留值为 null 的属性。 */
        private boolean includeNullValues = true;

        public Builder failOnUnknownProperties(boolean value) {
            this.failOnUnknownProperties = value;
            return this;
        }

        public Builder writeDatesAsTimestamps(boolean value) {
            this.writeDatesAsTimestamps = value;
            return this;
        }

        public Builder includeNullValues(boolean value) {
            this.includeNullValues = value;
            return this;
        }

        public JacksonConfiguration build() {
            return new JacksonConfiguration(this);
        }
    }
}
