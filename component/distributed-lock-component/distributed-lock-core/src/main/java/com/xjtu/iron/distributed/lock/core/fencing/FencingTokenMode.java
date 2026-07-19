package com.xjtu.iron.distributed.lock.core.fencing;

/** fencing token 的生成模式。 */
public enum FencingTokenMode {
    /** 本次不要求 fencing token。 */
    NONE,
    /** 由锁 Provider 在原子加锁流程中生成。 */
    NATIVE,
    /** 锁获取成功后由独立 FencingTokenProvider 生成。 */
    EXTERNAL
}
