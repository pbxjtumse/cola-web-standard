package com.xjtu.iron.idempotent.core.state;

/**
 * Repository 原子判定完成以后，Core 下一步应该做什么。
 */
public enum IdempotencyStateAction {

    /** 当前调用已经获得 PROCESSING generation，可以执行真实业务。 */
    EXECUTE,

    /** 历史状态已经 SUCCESS，不再执行 callback，进入结果回放。 */
    REPLAY,

    /** 不执行 callback，直接把稳定结果状态返回给上层。 */
    RETURN
}
