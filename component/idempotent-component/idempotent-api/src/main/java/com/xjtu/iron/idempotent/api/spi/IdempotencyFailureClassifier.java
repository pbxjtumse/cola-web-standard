package com.xjtu.iron.idempotent.api.spi;

import com.xjtu.iron.idempotent.api.repository.write.IdempotencyFailureInfo;

import java.time.Instant;

/**
 * 业务失败分类 SPI。
 *
 * <p>它只回答“FAILED 是否允许后续显式恢复”，不负责立即重试。
 * 普通 {@code execute()} 不会因为 retryable=true 就重新执行；
 * Reliable Task 必须显式调用 {@code recover()}。</p>
 */
public interface IdempotencyFailureClassifier {

    IdempotencyFailureInfo classify(Throwable error, Instant occurredAt);
}
