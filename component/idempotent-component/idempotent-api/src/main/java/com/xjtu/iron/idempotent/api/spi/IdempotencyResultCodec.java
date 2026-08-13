package com.xjtu.iron.idempotent.api.spi;

/**
 * 成功结果持久化与回放的序列化 SPI。
 *
 * <p>该 SPI 只在 {@code storeResult=true} 时使用。Core 不关心 JSON、ProtoBuf 或其他编码格式。
 * 结果存储是可选能力，幂等正确性本身不依赖结果回放。</p>
 */
public interface IdempotencyResultCodec {

    String encode(Object value) throws Exception;

    <T> T decode(String payload, Class<T> resultType) throws Exception;
}
