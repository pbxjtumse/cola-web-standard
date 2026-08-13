package com.xjtu.iron.idempotent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.iron.idempotent.api.spi.IdempotencyResultCodec;

/**
 * 使用 Jackson 保存/恢复幂等成功结果的默认 Codec。
 *
 * <p>只有 {@code storeResult=true} 时才会使用。结果快照属于幂等 replay 能力，
 * 不应默认存储超大对象、敏感数据或不可长期兼容的内部 Java 类型。</p>
 */
public final class JacksonIdempotencyResultCodec implements IdempotencyResultCodec {

    private final ObjectMapper mapper;

    public JacksonIdempotencyResultCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String encode(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    @Override
    public <T> T decode(String payload, Class<T> type) throws Exception {
        return mapper.readValue(payload, type);
    }
}
