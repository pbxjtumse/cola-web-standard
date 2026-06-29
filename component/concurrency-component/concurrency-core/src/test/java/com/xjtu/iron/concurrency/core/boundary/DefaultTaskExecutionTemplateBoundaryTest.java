package com.xjtu.iron.concurrency.core.boundary;

import com.xjtu.iron.concurrency.api.exception.ThreadPoolNotFoundException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.core.support.BlockingTask;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultTaskExecutionTemplate 边界测试")
class DefaultTaskExecutionTemplateBoundaryTest {

    @Test
    @DisplayName("submit：task 为 null 时应拒绝")
    void submit_should_reject_null_task() {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            assertThatThrownBy(() -> fixture.asyncExecutor().submit(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("task");
        }
    }

    @Test
    @DisplayName("run：runnable 为 null 时应拒绝")
    void run_should_reject_null_runnable() {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            assertThatThrownBy(() -> fixture.asyncExecutor().run("default", "bad-run", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("runnable");
        }
    }

    @Test
    @DisplayName("supply：supplier 为 null 时应在 AsyncTask 校验阶段拒绝")
    void supply_should_reject_null_supplier() {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            assertThatThrownBy(() -> fixture.asyncExecutor().supply("default", "bad-supply", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("operation");
        }
    }

    @Test
    @DisplayName("submit：线程池不存在时应抛出 ThreadPoolNotFoundException")
    void submit_should_fail_when_executor_not_found() {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            AsyncTask<String> task = AsyncTask.of("not-exist", "task", () -> "OK");

            assertThatThrownBy(() -> fixture.asyncExecutor().submit(task))
                    .isInstanceOf(ThreadPoolNotFoundException.class)
                    .hasMessageContaining("not-exist");
        }
    }

    @Test
    @DisplayName("submit：同一个 taskId 尚未完成时，重复提交应拒绝")
    void submit_should_reject_duplicate_running_task_id() throws Exception {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            BlockingTask<String> blocking = new BlockingTask<>("OK");

            AsyncTask<String> first = AsyncTask.of("default", "duplicate", blocking::get)
                    .taskId("DUP-001");
            CompletableFuture<String> firstFuture = fixture.asyncExecutor().submit(first);
            blocking.awaitStarted();

            AsyncTask<String> duplicate = AsyncTask.of("default", "duplicate", () -> "OTHER")
                    .taskId("DUP-001");

            assertThatThrownBy(() -> fixture.asyncExecutor().submit(duplicate))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DUP-001");

            blocking.release();
            firstFuture.join();
        }
    }
}
