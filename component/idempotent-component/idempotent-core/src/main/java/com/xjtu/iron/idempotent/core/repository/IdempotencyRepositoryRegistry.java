package com.xjtu.iron.idempotent.core.repository;

import com.xjtu.iron.idempotent.api.policy.IdempotencyMode;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

/** Repository 选择入口。 */
public interface IdempotencyRepositoryRegistry {

    IdempotencyRepository resolve(IdempotencyMode mode, String requestedProviderName);
}
