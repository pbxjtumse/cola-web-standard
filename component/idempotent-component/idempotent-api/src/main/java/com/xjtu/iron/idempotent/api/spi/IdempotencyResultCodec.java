package com.xjtu.iron.idempotent.api.spi;

import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicy;

/**
 * V1.2 结果编解码 SPI。
 *
 * @deprecated V1.3 结果保存已经改为类型安全的
 * {@code api.result.IdempotencyResultPolicy<T>}。
 * Executor 主 API 不再接收 Class<T>。
 */
@Deprecated
public interface IdempotencyResultCodec {

    String encode(Object value) throws Exception;

    <T> T decode(String payload, Class<T> resultType) throws Exception;
}
