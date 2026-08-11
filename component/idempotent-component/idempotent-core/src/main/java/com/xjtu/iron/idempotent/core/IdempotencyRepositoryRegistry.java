package com.xjtu.iron.idempotent.core;

import com.xjtu.iron.idempotent.api.IdempotencyMode;
import com.xjtu.iron.idempotent.api.repository.IdempotencyRepository;

public interface IdempotencyRepositoryRegistry {
    IdempotencyRepository resolve(IdempotencyMode mode, String requestedProviderName);
}
