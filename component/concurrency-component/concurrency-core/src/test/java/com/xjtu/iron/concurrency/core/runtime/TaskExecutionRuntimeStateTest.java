package com.xjtu.iron.concurrency.core.runtime;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.task.TaskExecutionMode;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.core.task.TaskExecutionRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskExecutionRuntime 状态机测试")
class TaskExecutionRuntimeStateTest {

    @Test
    @DisplayName("初始状态应为 CREATED、UNASSIGNED、RESULT_AWARE")
    void initial_state_should_be_created() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(null);

        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.CREATED);
        assertThat(runtime.getExecutionMode()).isEqualTo(TaskExecutionMode.UNASSIGNED);
        assertThat(runtime.getResultMode()).isEqualTo(TaskResultMode.RESULT_AWARE);
        assertThat(runtime.isBaseOutcomeResolved()).isFalse();
        assertThat(runtime.isFinalOutcomeResolved()).isFalse();
    }

    @Test
    @DisplayName("tryMarkSubmitted：只能从 CREATED 转为 SUBMITTED 一次")
    void tryMarkSubmitted_should_only_work_once() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertThat(runtime.tryMarkSubmitted()).isTrue();
        assertThat(runtime.tryMarkSubmitted()).isFalse();
        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.SUBMITTED);
    }

    @Test
    @DisplayName("tryMarkRunning：应标记运行线程和 THREAD_POOL 执行模式")
    void tryMarkRunning_should_set_thread_pool_mode() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);
        runtime.tryMarkSubmitted();

        assertThat(runtime.tryMarkRunning()).isTrue();

        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.RUNNING);
        assertThat(runtime.getExecutionMode()).isEqualTo(TaskExecutionMode.THREAD_POOL);
        assertThat(runtime.timingSnapshot().getStartTimeMillis()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("markCallerThreadExecution：应在运行前把执行模式标记为 CALLER_THREAD")
    void markCallerThreadExecution_should_set_caller_thread_mode() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        runtime.markCallerThreadExecution();
        runtime.tryMarkRunning();

        assertThat(runtime.getExecutionMode()).isEqualTo(TaskExecutionMode.CALLER_THREAD);
    }

    @Test
    @DisplayName("tryResolveBaseOutcome：原始结果只能被确定一次")
    void tryResolveBaseOutcome_should_only_win_once() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        boolean successWon = runtime.tryResolveBaseOutcome(AsyncTaskStatus.SUCCESS);
        boolean timeoutWon = runtime.tryResolveBaseOutcome(AsyncTaskStatus.TIMEOUT);
        boolean failedWon = runtime.tryResolveBaseOutcome(AsyncTaskStatus.FAILED);

        assertThat(successWon).isTrue();
        assertThat(timeoutWon).isFalse();
        assertThat(failedWon).isFalse();
        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.SUCCESS);
        assertThat(runtime.isBaseOutcomeResolved()).isTrue();
    }

    @Test
    @DisplayName("tryFinalize：最终结果只能被确定一次")
    void tryFinalize_should_only_win_once() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertThat(runtime.tryFinalize(AsyncTaskStatus.SUCCESS)).isTrue();
        assertThat(runtime.tryFinalize(AsyncTaskStatus.FALLBACK_SUCCESS)).isFalse();
        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.SUCCESS);
        assertThat(runtime.isFinalOutcomeResolved()).isTrue();
    }

    @Test
    @DisplayName("markIntermediate：最终状态确定后，不应再覆盖状态")
    void markIntermediate_should_not_override_final_status() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        runtime.tryFinalize(AsyncTaskStatus.SUCCESS);
        runtime.markIntermediate(AsyncTaskStatus.FALLBACK);

        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.SUCCESS);
    }

    @Test
    @DisplayName("tryCancel：取消成功后，base 和 final 都应完成，后续成功不能覆盖取消")
    void tryCancel_should_resolve_base_and_final_outcome() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertThat(runtime.tryCancel()).isTrue();
        assertThat(runtime.tryResolveBaseOutcome(AsyncTaskStatus.SUCCESS)).isFalse();
        assertThat(runtime.tryFinalize(AsyncTaskStatus.SUCCESS)).isFalse();

        assertThat(runtime.getStatus()).isEqualTo(AsyncTaskStatus.CANCELLED);
        assertThat(runtime.isBaseOutcomeResolved()).isTrue();
        assertThat(runtime.isFinalOutcomeResolved()).isTrue();
    }

    @Test
    @DisplayName("isQueueTimeout：未配置、0、负数都不应判定为排队超时")
    void isQueueTimeout_should_ignore_invalid_timeout() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertThat(runtime.isQueueTimeout(null)).isFalse();
        assertThat(runtime.isQueueTimeout(Duration.ZERO)).isFalse();
        assertThat(runtime.isQueueTimeout(Duration.ofMillis(-1))).isFalse();
    }

    @Test
    @DisplayName("tryResolveBaseOutcome / tryFinalize 入参为 null 时应拒绝")
    void terminal_methods_should_reject_null_status() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertThatThrownBy(() -> runtime.tryResolveBaseOutcome(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseStatus");
        assertThatThrownBy(() -> runtime.tryFinalize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("finalStatus");
    }
}
