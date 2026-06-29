package com.xjtu.iron.concurrency.core.functional;

import com.xjtu.iron.concurrency.api.execution.executor.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.core.support.RecordingTaskExecutionListener;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultAsyncExecutor 功能测试")
class DefaultAsyncExecutorFunctionalTest {

    @Test
    @DisplayName("supply：任务正常返回时，Future 应返回业务结果")
    void supply_should_return_result_when_task_success() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AsyncExecutor executor = fixture.asyncExecutor();

            CompletableFuture<String> future = executor.supply("default", "supply-success", () -> "OK");

            assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo("OK");
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "SUCCESS", "COMPLETED:SUCCESS"
            );
        }
    }

    @Test
    @DisplayName("run：无返回值任务正常执行时，Future 应正常完成")
    void run_should_complete_when_runnable_success() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AtomicBoolean executed = new AtomicBoolean(false);

            CompletableFuture<Void> future = fixture.asyncExecutor().run(
                    "default",
                    "run-success",
                    () -> executed.set(true)
            );

            future.get(2, TimeUnit.SECONDS);

            assertThat(executed.get()).isTrue();
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "SUCCESS", "COMPLETED:SUCCESS"
            );
        }
    }

    @Test
    @DisplayName("submit：完整 AsyncTask 支持 timeout、bizKey、tag 等元数据")
    void submit_should_accept_full_async_task_model() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AsyncTask<String> task = AsyncTask.of("default", "full-task", () -> "VALUE")
                    .taskId("FULL-001")
                    .bizKey("orderId=20260629")
                    .tag("scene", "unit-test")
                    .timeout(Duration.ofSeconds(2));

            String result = fixture.asyncExecutor().submit(task).get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("VALUE");
            assertThat(fixture.taskExecutionRegistry().get("FULL-001")).isPresent();
            assertThat(fixture.taskExecutionRegistry().get("FULL-001").orElseThrow().getTask().getBizKey())
                    .isEqualTo("orderId=20260629");
        }
    }

    @Test
    @DisplayName("submit：原始任务失败且 fallback 成功时，应返回 fallback 结果")
    void submit_should_return_fallback_result_when_primary_failed() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AsyncTask<String> task = AsyncTask.<String>of("default", "fallback-success", () -> {
                        throw new IllegalStateException("primary failed");
                    })
                    .taskId("FB-001")
                    .fallback(error -> "FALLBACK");

            String result = fixture.asyncExecutor().submit(task).get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("FALLBACK");
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "FAILED", "FALLBACK", "FALLBACK_SUCCESS", "COMPLETED:FALLBACK_SUCCESS"
            );
        }
    }

    @Test
    @DisplayName("submit：fallback 自身失败时，最终 Future 应异常完成")
    void submit_should_fail_when_fallback_failed() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AsyncTask<String> task = AsyncTask.<String>of("default", "fallback-failed", () -> {
                        throw new IllegalStateException("primary failed");
                    })
                    .taskId("FB-002")
                    .fallback(error -> {
                        throw new IllegalArgumentException("fallback failed");
                    });

            CompletableFuture<String> future = fixture.asyncExecutor().submit(task);

            assertThatThrownByFutureGet(future);
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "FAILED", "FALLBACK", "FALLBACK_FAILED", "COMPLETED:FALLBACK_FAILED"
            );
        }
    }

    private static void assertThatThrownByFutureGet(CompletableFuture<?> future) throws Exception {
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (Exception expected) {
            assertThat(future.isCompletedExceptionally()).isTrue();
            return;
        }
        throw new AssertionError("future should complete exceptionally");
    }
}
