package com.xjtu.iron.idempotent.api.result;

import java.util.Objects;

/**
 * 常用 ResultPolicy 工厂。
 */
public final class IdempotencyResultPolicies {

    private static final IdempotencyResultPolicy<Object> NONE = new IdempotencyResultPolicy<>() {
        @Override
        public IdempotencyResultPolicyType type() {
            return IdempotencyResultPolicyType.NONE;
        }

        @Override
        public String capture(Object value) {
            return null;
        }

        @Override
        public Object replay(String storedValue) {
            return null;
        }

        @Override
        public boolean requiresPayloadForReplay() {
            return false;
        }
    };

    private IdempotencyResultPolicies() {
    }

    @SuppressWarnings("unchecked")
    public static <T> IdempotencyResultPolicy<T> none() {
        return (IdempotencyResultPolicy<T>) NONE;
    }

    public static <T> IdempotencyResultPolicy<T> snapshot(
            IdempotencyResultSerializer<T> serializer) {
        Objects.requireNonNull(serializer, "serializer must not be null");
        return new IdempotencyResultPolicy<>() {
            @Override
            public IdempotencyResultPolicyType type() {
                return IdempotencyResultPolicyType.SNAPSHOT;
            }

            @Override
            public String capture(T value) throws Exception {
                return serializer.serialize(value);
            }

            @Override
            public T replay(String storedValue) throws Exception {
                return serializer.deserialize(storedValue);
            }
        };
    }

    public static <T> IdempotencyResultPolicy<T> reference(
            IdempotencyResultReference<T> reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return new IdempotencyResultPolicy<>() {
            @Override
            public IdempotencyResultPolicyType type() {
                return IdempotencyResultPolicyType.REFERENCE;
            }

            @Override
            public String capture(T value) throws Exception {
                return reference.capture(value);
            }

            @Override
            public T replay(String storedValue) throws Exception {
                return reference.resolve(storedValue);
            }
        };
    }
}
