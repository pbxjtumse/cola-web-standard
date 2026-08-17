package com.xjtu.iron.idempotent.api.result;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 框架无关的泛型类型令牌。
 *
 * <p>示例：</p>
 * <pre>
 * new IdempotencyTypeRef&lt;List&lt;OrderResult&gt;&gt;() {}
 * </pre>
 *
 * <p>API 不依赖 Jackson 的 TypeReference / JavaType。</p>
 */
public abstract class IdempotencyTypeRef<T> {

    private final Type type;

    protected IdempotencyTypeRef() {
        Type generic = getClass().getGenericSuperclass();
        if (!(generic instanceof ParameterizedType parameterized)) {
            throw new IllegalStateException(
                    "IdempotencyTypeRef must be created with an anonymous generic subclass");
        }
        this.type = parameterized.getActualTypeArguments()[0];
    }

    public final Type getType() {
        return type;
    }
}
