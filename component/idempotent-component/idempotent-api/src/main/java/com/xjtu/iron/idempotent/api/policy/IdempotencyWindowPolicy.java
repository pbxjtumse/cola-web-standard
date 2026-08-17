package com.xjtu.iron.idempotent.api.policy;

/**
 * WINDOWED 幂等窗口的推进策略。
 */
public enum IdempotencyWindowPolicy {

    /**
     * 从第一次成功抢占开始固定计算窗口。
     * 后续 SUCCESS / FAILED / 重复访问都不会延长语义窗口。
     */
    FIXED_FROM_FIRST_ACQUIRE,

    /**
     * 滑动窗口。
     * 在窗口仍有效时，每次有效幂等访问/状态完成都会把 windowExpireAt 推进到 now + idempotencyWindow。
     *
     * <p>适合“最近 N 分钟内持续去重”的接口场景。</p>
     */
    SLIDING_ON_ACCESS
}
