package com.xjtu.iron.idempotent.api.result;

import java.util.Objects;

/**
 * 常用 ResultPolicy 工厂。
 *
 * <p>三种策略只回答“历史已经 SUCCESS 时，重复请求返回什么”，不参与 owner 抢占、Recovery 或事务传播。</p>
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

    /**
     * 只保存“历史成功”这个事实，不保存业务返回值。适合 MQ 消费、后台任务等只需要防重的场景。
     */
    @SuppressWarnings("unchecked")
    public static <T> IdempotencyResultPolicy<T> none() {
        return (IdempotencyResultPolicy<T>) NONE;
    }

    /**
     * 保存第一次成功返回值的快照，重复请求直接反序列化同一份快照。
     */
    public static <T> IdempotencyResultPolicy<T> snapshot(IdempotencyResultSerializer<T> serializer) {
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

    /**
     * 只保存稳定业务引用，例如 orderId；重复请求通过该引用重新查询/组装业务结果。
     * 对长期 DURABLE 记录通常比永久保存 DTO JSON 更稳定。
     */
    public static <T> IdempotencyResultPolicy<T> reference(IdempotencyResultReference<T> reference) {
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
