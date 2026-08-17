package com.xjtu.iron.idempotent.api.result;

/**
 * 把具体序列化框架适配成类型安全 SNAPSHOT ResultPolicy 的 SPI。
 *
 * <p>Starter 默认提供 Jackson 实现，业务代码只依赖 idempotent-api。</p>
 */
public interface IdempotencySnapshotPolicyFactory {

    <T> IdempotencyResultPolicy<T> snapshot(IdempotencyTypeRef<T> typeRef);
}
