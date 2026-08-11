package com.xjtu.iron.idempotent.api.spi;public interface IdempotencyResultCodec{String encode(Object value)throws Exception;<T>T decode(String payload,Class<T> resultType)throws Exception;}
