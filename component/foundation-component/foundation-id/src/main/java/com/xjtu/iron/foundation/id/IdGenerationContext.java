package com.xjtu.iron.foundation.id;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 描述生成技术标识时的命名空间和附加属性。
 */
public final class IdGenerationContext {

    /** 标识所属的技术命名空间。 */
    private final String namespace;
    /** 生成标识时可选的附加属性。 */
    private final Map<String, String> attributes;

    public IdGenerationContext(String namespace, Map<String, String> attributes) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        this.namespace = namespace;
        this.attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String getNamespace() { return namespace; }
    public Map<String, String> getAttributes() { return attributes; }

    public String requireAttribute(String name) {
        String value = attributes.get(Objects.requireNonNull(name, "name must not be null"));
        if (value == null) {
            throw new IllegalArgumentException("missing id generation attribute: " + name);
        }
        return value;
    }
}
