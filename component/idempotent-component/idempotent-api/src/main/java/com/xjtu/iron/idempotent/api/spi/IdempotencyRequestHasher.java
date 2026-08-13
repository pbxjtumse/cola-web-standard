package com.xjtu.iron.idempotent.api.spi;

/**
 * 把“真正决定业务语义的请求指纹对象”转换成稳定 requestHash。
 *
 * <p>不要默认把 traceId、timestamp、nonce、签名等每次重试都会变化的字段放进指纹对象。</p>
 */
@FunctionalInterface
public interface IdempotencyRequestHasher {
    String hash(Object businessFingerprint);
}
