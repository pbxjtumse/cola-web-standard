package com.xjtu.iron.distributed.lock.core.acquire.outcome;

import com.xjtu.iron.distributed.lock.api.LockHandle;
import com.xjtu.iron.distributed.lock.api.LockResult;
import com.xjtu.iron.distributed.lock.core.spi.status.LockAcquireStatus;

/** 根据 Provider acquire 状态解释并生成最终 Core 结果。 */
public interface LockAcquireOutcomeHandler {

    /** 当前处理器负责的唯一 Provider 状态。 */
    LockAcquireStatus status();

    /** 解释 acquire 响应并返回 API 层结果。 */
    LockResult<LockHandle> handle(LockAcquireOutcomeContext context);
}
