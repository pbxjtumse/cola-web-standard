package com.xjtu.iron.idempotent.api.context;

import com.xjtu.iron.idempotent.api.result.IdempotencyResultStatus;

/**
 * 一次幂等调用所处的处理阶段。
 *
 * <p>Stage 回答“结果在哪个阶段产生”，与 {@link IdempotencyResultStatus} 分离。
 * 例如 REPOSITORY_ERROR 既可能发生在状态抢占，也可能发生在最终完成状态写入。</p>
 */
public enum IdempotencyStage {

    /** 请求、Policy、ResultPolicy、Repository 能力校验阶段。 */
    VALIDATE,

    /** 可选 DistributedLockClient 的极短协调阶段。 */
    LOCK,

    /** 普通 execute() 的状态抢占/判定阶段。 */
    ACQUIRE_STATE,

    /** recover() 的显式恢复抢占阶段。 */
    RECOVER_STATE,

    /** 真正业务 callback 执行阶段。 */
    EXECUTE,

    /** Tx-B 业务事务建立、完成或提交结果判定阶段。 */
    TRANSACTION,

    /** markSuccess / markFailed 等最终状态写入阶段。 */
    COMPLETE_STATE,

    /** SUCCESS 历史结果回放阶段。 */
    REPLAY
}
