package com.xjtu.iron.concurrency.core.ordering;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.execution.task.TaskCancelResult;
import com.xjtu.iron.concurrency.api.execution.task.TaskHandle;
import com.xjtu.iron.concurrency.core.support.BlockingTask;
import com.xjtu.iron.concurrency.core.support.RecordingTaskExecutionListener;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Timeout / Fallback / Cancel 时序测试")
class TimeoutFallbackCancelOrderTest {

    @Test
    @DisplayName("结果层超时：TIMEOUT 赢后，业务线程晚到成功不能覆盖最终状态")
    void timeout_should_win_and_late_success_should_not_override_it() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            BlockingTask<String> blocking = new BlockingTask<>("LATE-SUCCESS");
            AsyncTask<String> task = AsyncTask.of("default", "timeout-win", blocking::get)
                    .taskId("TIMEOUT-001")
                    .timeout(Duration.ofMillis(80))
                    .cancelOnTimeout(false);

            CompletableFuture<String> future = fixture.asyncExecutor().submit(task);
            blocking.awaitStarted();

            assertFutureFailed(future);

            blocking.release();
            Thread.sleep(80L);

            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "TIMEOUT", "COMPLETED:TIMEOUT"
            );
            assertThat(listener.count(AsyncTaskStatus.SUCCESS)).isZero();
            assertThat(fixture.taskExecutionRegistry().get("TIMEOUT-001").orElseThrow().getStatus())
                    .isEqualTo(AsyncTaskStatus.TIMEOUT);
        }
    }

    @Test
    @DisplayName("结果层超时 + fallback：TIMEOUT 后应触发 fallback，并以 FALLBACK_SUCCESS 作为最终状态")
    void timeout_should_trigger_fallback_when_fallback_configured() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            BlockingTask<String> blocking = new BlockingTask<>("LATE-SUCCESS");
            AsyncTask<String> task = AsyncTask.of("default", "timeout-fallback", blocking::get)
                    .taskId("TIMEOUT-FB-001")
                    .timeout(Duration.ofMillis(80))
                    .fallback(error -> "TIMEOUT-FALLBACK");

            CompletableFuture<String> future = fixture.asyncExecutor().submit(task);
            blocking.awaitStarted();

            String result = future.get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("TIMEOUT-FALLBACK");
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "TIMEOUT", "FALLBACK", "FALLBACK_SUCCESS", "COMPLETED:FALLBACK_SUCCESS"
            );
            assertThat(listener.events()).doesNotContain("COMPLETED:TIMEOUT");
            blocking.release();
        }
    }

    @Test
    @DisplayName("主动取消：应发布 CANCELLED 和 COMPLETED:CANCELLED，并取消最终 Future")
    void cancel_should_publish_cancelled_and_cancel_final_future() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create(listener)) {
            BlockingTask<String> blocking = new BlockingTask<>("SHOULD_NOT_RETURN");
            AsyncTask<String> task = AsyncTask.of("default", "cancel-task", blocking::get)
                    .taskId("CANCEL-001");

            TaskHandle<String> handle = fixture.asyncExecutor().submitHandle(task);
            blocking.awaitStarted();

            TaskCancelResult result = handle.cancel(true);

            assertThat(result).isEqualTo(TaskCancelResult.CANCELLED);
            assertThat(handle.getFuture().isCancelled()).isTrue();
            assertThat(listener.events()).containsSubsequence(
                    "SUBMITTED", "RUNNING", "CANCELLED", "COMPLETED:CANCELLED"
            );
            await().atMost(Duration.ofSeconds(1))
                    .untilAsserted(() -> assertThat(blocking.wasInterrupted()).isTrue());
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
