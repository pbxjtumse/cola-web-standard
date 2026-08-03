package com.xjtu.iron.foundation.serialization;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 按数据格式管理序列化器。
 */
public final class SerializerRegistry {

    /** 按数据格式保存的序列化器集合。 */
    private final Map<SerializationFormat, Serializer> serializers;

    public SerializerRegistry(Iterable<? extends Serializer> serializers) {
        EnumMap<SerializationFormat, Serializer> registered = new EnumMap<>(SerializationFormat.class);
        if (serializers != null) {
            for (Serializer serializer : serializers) {
                if (serializer == null) {
                    continue;
                }
                Serializer previous = registered.putIfAbsent(serializer.format(), serializer);
                if (previous != null) {
                    throw new IllegalArgumentException("duplicate serializer format: " + serializer.format());
                }
            }
        }
        this.serializers = Collections.unmodifiableMap(registered);
    }

    public Serializer require(SerializationFormat format) {
        Serializer serializer = serializers.get(format);
        if (serializer == null) {
            throw new SerializerNotFoundException("serializer not found for format: " + format);
        }
        return serializer;
    }

    public Map<SerializationFormat, Serializer> getSerializers() {
        return serializers;
    }
}
