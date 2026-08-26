package com.xjtu.iron.distributed.lock.core.fencing.flow;

import com.xjtu.iron.distributed.lock.core.fencing.coordinator.FencingTokenMode;

/**
 * 不启用 fencing 的空流程。
 *
 * <p>把 NONE 也抽成 Flow，是为了让 Acquire 成功后的处理路径完全统一：无 fencing、native fencing、external fencing 都返回
 * {@link FencingCompletion}，AcquireOutcomeHandler 不需要再写 if/else。</p>
 */
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
