package com.xjtu.iron.distributed.lock.core.acquire;

import com.xjtu.iron.distributed.lock.api.model.LockHandle;
import com.xjtu.iron.distributed.lock.api.model.LockResult;
import com.xjtu.iron.distributed.lock.spi.protocol.acquire.LockAcquireStatus;

import java.util.Optional;
import java.util.Set;

/** acquire 结果处理器注册表。 */
public interface LockAcquireOutcomeHandlerRegistry {

    Optional<LockAcquireOutcomeHandler> findHandler(LockAcquireStatus status);

    LockAcquireOutcomeHandler getRequired(LockAcquireStatus status);

    LockResult<LockHandle> handle(LockAcquireOutcomeContext context);

    Set<LockAcquireStatus> statuses();
}
