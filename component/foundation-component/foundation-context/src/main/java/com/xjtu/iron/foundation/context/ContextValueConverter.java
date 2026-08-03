package com.xjtu.iron.foundation.context;

/**
 * 将上下文值转换为可跨进程传输的字符串。
 */
public interface ContextValueConverter<T> {

    String serialize(T value);

    T deserialize(String value);
}
