package com.xjtu.iron.idempotent.api.operation;

import com.xjtu.iron.idempotent.api.operation.acquire.IdempotencyOperationAcquireCommand;
import com.xjtu.iron.idempotent.api.operation.acquire.IdempotencyOperationAcquireResult;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyCompletionCommand;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyFailureCommand;
import com.xjtu.iron.idempotent.api.operation.write.IdempotencyOperationWriteResult;

/**
 * 面向 message/task/workflow 等技术组件的低层幂等状态 API。
 *
 * <p>{@code IdempotencyExecutor} 负责“acquire -> callback -> final state”的高级业务模板；
 * 本接口只暴露状态操作，让拥有自己业务编排/事务/ACK 生命周期的技术组件显式控制 acquire 与终态写入。</p>
 *
 * <p>调用方不会直接依赖 JDBC/Redis Repository，也不会感知真实表名或未来分库分表规则。</p>
 */
public interface IdempotencyOperations {

    IdempotencyOperationAcquireResult acquire(IdempotencyOperationAcquireCommand command);

    IdempotencyOperationWriteResult markSuccess(IdempotencyCompletionCommand command);

    IdempotencyOperationWriteResult markFailed(IdempotencyFailureCommand command);

    IdempotencyOperationWriteResult markDiscarded(IdempotencyCompletionCommand command);
}
