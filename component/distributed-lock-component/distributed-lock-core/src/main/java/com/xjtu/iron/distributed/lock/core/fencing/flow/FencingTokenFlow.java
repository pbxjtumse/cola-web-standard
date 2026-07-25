package com.xjtu.iron.distributed.lock.core.fencing.flow;

import com.xjtu.iron.distributed.lock.core.fencing.FencingTokenMode;

/**
 * fencing token 执行流程策略。
 *
 * <p>该接口只表达“某一种 fencing 模式拿到锁之后应该如何补全/校验 token”。
 * 锁 Provider 的选择由 LockProviderRegistry 负责；外部发号 Provider 的选择由
 * FencingTokenCoordinator/FencingTokenProviderRegistry 负责；这里负责执行流程。</p>
 */
public interface FencingTokenFlow {

    /** 当前策略支持的 fencing 模式。 */
    FencingTokenMode mode();

    /**
     * 完成当前 fencing 模式的后置处理。
     *
     * @param context fencing 执行上下文。
     * @return 成功时返回补全后的 LockLease；失败时返回最终 LockResult。
     */
    FencingCompletion complete(FencingContext context);
}
