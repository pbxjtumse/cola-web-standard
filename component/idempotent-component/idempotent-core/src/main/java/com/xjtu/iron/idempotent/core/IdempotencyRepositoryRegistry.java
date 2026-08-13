package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

/**
 * Repository 选择入口。
 *
 * <p>调用方可以显式指定 providerName；未指定时按 SHORT_TERM / DURABLE 模式使用默认 Provider。
 * Registry 只处理选择与能力校验，不参与状态转换。</p>
 */
public interface IdempotencyRepositoryRegistry {

    IdempotencyRepository resolve(IdempotencyMode mode, String requestedProviderName);
}
