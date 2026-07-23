package com.xjtu.iron.distributed.lock.core.acquire.outcome;

import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.core.spi.status.LockAcquireStatus;

import java.util.Optional;
import java.util.Set;

/** acquire 结果处理器注册表。 */
public interface LockAcquireOutcomeHandlerRegistry {

    Optional<LockAcquireOutcomeHandler> findHandler(LockAcquireStatus status);

    LockAcquireOutcomeHandler getRequired(LockAcquireStatus status);

    LockResult<LockHandle> handle(LockAcquireOutcomeContext context);

    Set<LockAcquireStatus> statuses();
}
