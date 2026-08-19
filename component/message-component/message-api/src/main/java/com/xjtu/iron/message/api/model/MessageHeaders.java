package com.xjtu.iron.message.api.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 保存业务自定义头和可传播技术头的不可变值对象。
 *
 * <p>该对象不保存 x-iron-message-* 系统头；系统头只在线级编码阶段由 core 生成。</p>
 */
public final class MessageHeaders {

    private static final MessageHeaders EMPTY = new MessageHeaders(Map.of());

    private final Map<String, String> values;

    private MessageHeaders(Map<String, String> values) {
        this.values = values;
    }

    /** 返回空消息头。 */
    public static MessageHeaders empty() {
        return EMPTY;
    }

    /** 从 Map 创建不可变消息头。 */
    public static MessageHeaders of(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return EMPTY;
        }
        Map<String, String> copy = new LinkedHashMap<>();
        headers.forEach((name, value) -> {
            validate(name, value);
            copy.put(name.trim(), value);
        });
        return new MessageHeaders(Collections.unmodifiableMap(copy));
    }

    /** 返回只读 Map 视图。 */
    public Map<String, String> asMap() {
        return values;
    }

    /** 读取指定消息头。 */
    public Optional<String> get(String name) {
        return Optional.ofNullable(values.get(name));
    }

    /** 判断是否为空。 */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** 返回添加或覆盖一个用户消息头后的新对象。 */
    public MessageHeaders with(String name, String value) {
        validate(name, value);
        Map<String, String> copy = new LinkedHashMap<>(values);
        copy.put(name.trim(), value);
        return new MessageHeaders(Collections.unmodifiableMap(copy));
    }

    private static void validate(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("message header name must not be blank");
        }
        if (MessageHeaderNames.isReserved(name.trim())) {
            throw new IllegalArgumentException("reserved message header: " + name);
        }
        if (value == null) {
            throw new IllegalArgumentException("message header value must not be null: " + name);
        }
    }

    @Override
    public boolean equals(Object object) {
        return object == this || object instanceof MessageHeaders other && values.equals(other.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
