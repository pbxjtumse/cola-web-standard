package com.xjtu.iron.foundation.context;

/**
 * 字符串上下文值转换器。
 */
public final class StringContextValueConverter implements ContextValueConverter<String> {

    public static final StringContextValueConverter INSTANCE = new StringContextValueConverter();

    private StringContextValueConverter() {
    }

    @Override
    public String serialize(String value) {
        return value;
    }

    @Override
    public String deserialize(String value) {
        return value;
    }
}
