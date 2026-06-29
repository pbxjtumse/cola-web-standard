package com.xjtu.iron.concurrency.core.ordering;

import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.core.support.RecordingTaskExecutionListener;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultAsyncExecutor 生命周期事件顺序测试")
class DefaultAsyncExecutorEventOrderTest {

    @Test
    @DisplayName("成功路径：事件顺序应为 SUBMITTED -> RUNNING -> SUCCESS -> COMPLETED:SUCCESS")
    void success_events_should_be_ordered() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            String result = fixture.asyncExecutor()
                    .supply("default", "success-order", () -> "OK")
                    .get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("OK");
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "SUCCESS", "COMPLETED:SUCCESS"
            );
            assertThat(listener.count(com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus.SUCCESS)).isEqualTo(1);
            assertThat(listener.completedCount(com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus.SUCCESS)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("失败无 fallback：事件顺序应为 SUBMITTED -> RUNNING -> FAILED -> COMPLETED:FAILED")
    void failed_without_fallback_events_should_be_ordered() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            CompletableFuture<String> future = fixture.asyncExecutor().supply("default", "failed-order", () -> {
                throw new IllegalStateException("failed");
            });

            assertFutureFailed(future);
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "FAILED", "COMPLETED:FAILED"
            );
        }
    }

    @Test
    @DisplayName("失败有 fallback：FAILED 是原始结果，FALLBACK_SUCCESS 才是最终结果")
    void fallback_success_events_should_be_ordered() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            AsyncTask<String> task = AsyncTask.<String>of("default", "fallback-order", () -> {
                        throw new IllegalStateException("failed");
                    })
                    .taskId("ORDER-FB-001")
                    .fallback(error -> "FALLBACK");

            String result = fixture.asyncExecutor().submit(task).get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("FALLBACK");
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "FAILED", "FALLBACK", "FALLBACK_SUCCESS", "COMPLETED:FALLBACK_SUCCESS"
            );
            assertThat(listener.events()).doesNotContain("COMPLETED:FAILED");
        }
    }

    private static void assertFutureFailed(CompletableFuture<?> future) throws Exception {
        try {
            future.get(2, TimeUnit.SECONDS);
        } catch (Exception expected) {
            assertThat(future.isCompletedExceptionally()).isTrue();
            return;
        }
        throw new AssertionError("future should complete exceptionally");
    }
}
