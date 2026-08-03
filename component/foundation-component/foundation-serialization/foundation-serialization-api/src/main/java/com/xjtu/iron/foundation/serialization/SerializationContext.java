package com.xjtu.iron.foundation.serialization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 描述一次序列化调用的技术上下文。
 */
public final class SerializationContext {

    /** 无附加信息时复用的空序列化上下文。 */
    private static final SerializationContext EMPTY = new SerializationContext(null, null, Map.of());

    /** 发起序列化操作的技术操作名称。 */
    private final String operationName;
    /** 载荷协议或 Schema 版本。 */
    private final String schemaVersion;
    /** 序列化过程使用的低基数附加属性。 */
    private final Map<String, String> attributes;

    public SerializationContext(String operationName, String schemaVersion, Map<String, String> attributes) {
        this.operationName = operationName;
        this.schemaVersion = schemaVersion;
        this.attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static SerializationContext empty() { return EMPTY; }
    public String getOperationName() { return operationName; }
    public String getSchemaVersion() { return schemaVersion; }
    public Map<String, String> getAttributes() { return attributes; }
}
