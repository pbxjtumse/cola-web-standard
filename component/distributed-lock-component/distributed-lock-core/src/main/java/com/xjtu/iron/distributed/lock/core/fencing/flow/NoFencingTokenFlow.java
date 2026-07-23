package com.xjtu.iron.distributed.lock.core.fencing.flow;

import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenMode;

/** 不要求 fencing token：直接使用 Provider 返回的 LockLease。 */
public final class NoFencingTokenFlow implements FencingTokenFlow {

    @Override
    public FencingTokenMode mode() {
        return FencingTokenMode.NONE;
    }

    @Override
    public FencingCompletion complete(FencingContext context) {
        return FencingCompletion.success(context.lease());
    }
}
