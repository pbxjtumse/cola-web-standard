package com.xjtu.iron.concurrency.core.functional;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.core.support.BlockingTask;
import com.xjtu.iron.concurrency.core.support.RecordingTaskExecutionListener;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import com.xjtu.iron.concurrency.core.support.TestExecutors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("线程池拒绝功能测试")
class ThreadPoolRejectionFunctionalTest {

    @Test
    @DisplayName("ABORT 拒绝策略：Future 应异常完成，监听器应收到 REJECTED 和 COMPLETED:REJECTED")
    void abort_rejection_should_complete_future_exceptionally_and_publish_rejected() throws Exception {
        RecordingTaskExecutionListener listener = new RecordingTaskExecutionListener();
        ThreadPoolExecutor smallExecutor = TestExecutors.smallRejecting("small");

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create("small", smallExecutor, listener)) {
            BlockingTask<String> running = new BlockingTask<>("RUNNING");
            BlockingTask<String> queued = new BlockingTask<>("QUEUED");

            CompletableFuture<String> runningFuture = fixture.asyncExecutor().supply("small", "running", running::get);
            running.awaitStarted();
            CompletableFuture<String> queuedFuture = fixture.asyncExecutor().supply("small", "queued", queued::get);

            CompletableFuture<String> rejectedFuture = fixture.asyncExecutor().supply("small", "rejected", () -> "SHOULD_NOT_RUN");

            assertFutureFailed(rejectedFuture);
            assertThat(listener.events()).contains("REJECTED", "COMPLETED:REJECTED");
            assertThat(listener.count(AsyncTaskStatus.REJECTED)).isEqualTo(1);
            assertThat(listener.completedCount(AsyncTaskStatus.REJECTED)).isEqualTo(1);

            running.release();
            queued.release();
            runningFuture.get(2, TimeUnit.SECONDS);
            queuedFuture.get(2, TimeUnit.SECONDS);
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
