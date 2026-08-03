package com.xjtu.iron.foundation.test.serialization;

import com.xjtu.iron.foundation.serialization.Serializer;

/**
 * 提供不绑定 JUnit 的序列化往返断言。
 */
public final class SerializationAssertions {

    private SerializationAssertions() {
    }

    public static <T> T roundTrip(Serializer serializer, T value, Class<T> targetType) {
        byte[] content = serializer.serialize(value);
        T restored = serializer.deserialize(content, targetType);
        if (!java.util.Objects.equals(value, restored)) {
            throw new AssertionError("serialization round trip produced a different value");
        }
        return restored;
    }
}
