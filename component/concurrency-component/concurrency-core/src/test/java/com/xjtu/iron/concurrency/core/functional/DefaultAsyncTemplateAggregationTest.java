package com.xjtu.iron.concurrency.core.functional;

import com.xjtu.iron.concurrency.api.execution.template.AsyncBatchResult;
import com.xjtu.iron.concurrency.api.execution.template.AsyncTemplate;
import com.xjtu.iron.concurrency.api.execution.template.NamedFuture;
import com.xjtu.iron.concurrency.core.async.DefaultAsyncTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DefaultAsyncTemplate 编排功能测试")
class DefaultAsyncTemplateAggregationTest {

    private ScheduledThreadPoolExecutor timeoutScheduler;
    private AsyncTemplate template;

    @BeforeEach
    void setUp() {
        timeoutScheduler = new ScheduledThreadPoolExecutor(1);
        timeoutScheduler.setRemoveOnCancelPolicy(true);
        template = new DefaultAsyncTemplate(timeoutScheduler);
    }

    @AfterEach
    void tearDown() {
        timeoutScheduler.shutdownNow();
    }

    @Test
    @DisplayName("allOf：全部成功时，应按传入顺序返回结果")
    void allOf_should_return_values_in_input_order() throws Exception {
        CompletableFuture<List<String>> future = template.allOf(List.of(
                CompletableFuture.completedFuture("A"),
                CompletableFuture.completedFuture("B")
        ));

        assertThat(future.get(1, TimeUnit.SECONDS)).containsExactly("A", "B");
    }

    @Test
    @DisplayName("allOf：任意一个 Future 失败时，整体应失败")
    void allOf_should_fail_when_any_future_failed() {
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("boom"));

        CompletableFuture<List<String>> future = template.allOf(List.of(
                CompletableFuture.completedFuture("A"),
                failed
        ));

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("allOfOutcome：允许部分失败，并返回成功值和失败明细")
    void allOfOutcome_should_collect_success_and_failure() throws Exception {
        CompletableFuture<String> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalArgumentException("bad request"));

        CompletableFuture<AsyncBatchResult<String>> future = template.allOfOutcome(List.of(
                NamedFuture.of("user", CompletableFuture.completedFuture("U")),
                NamedFuture.of("order", failed)
        ));

        AsyncBatchResult<String> result = future.get(1, TimeUnit.SECONDS);

        assertThat(result.isAllSuccess()).isFalse();
        assertThat(result.hasFailure()).isTrue();
        assertThat(result.successValues()).containsExactly("U");
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).getTaskName()).isEqualTo("order");
    }


    @Test
    @DisplayName("allOfFailFast：任意 Future 失败时，应立即整体失败并尽力取消其他 Future")
    void allOfFailFast_should_fail_fast_and_cancel_others() {
        CompletableFuture<String> failed = new CompletableFuture<>();
        CompletableFuture<String> pending = new CompletableFuture<>();

        CompletableFuture<List<String>> result = template.allOfFailFast(List.of(failed, pending));
        failed.completeExceptionally(new IllegalStateException("fast failed"));

        assertThat(result.isCompletedExceptionally()).isTrue();
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("anyOf：第一个完成的 Future 失败时，整体也应失败")
    void anyOf_should_fail_when_first_completed_future_failed() {
        CompletableFuture<String> failed = new CompletableFuture<>();
        CompletableFuture<String> slowSuccess = new CompletableFuture<>();

        CompletableFuture<String> result = template.anyOf(List.of(failed, slowSuccess));
        failed.completeExceptionally(new IllegalStateException("first failed"));
        slowSuccess.complete("OK");

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    @DisplayName("anySuccess：应忽略失败 Future，返回第一个成功值")
    void anySuccess_should_ignore_failures_and_return_first_success() throws Exception {
        CompletableFuture<String> failed = new CompletableFuture<>();
        CompletableFuture<String> success = new CompletableFuture<>();

        CompletableFuture<String> result = template.anySuccess(List.of(failed, success));
        failed.completeExceptionally(new IllegalStateException("failed"));
        success.complete("OK");

        assertThat(result.get(1, TimeUnit.SECONDS)).isEqualTo("OK");
    }

    @Test
    @DisplayName("withTimeout：source 长时间未完成时，包装 Future 应超时失败")
    void withTimeout_should_fail_when_source_not_completed_in_time() {
        CompletableFuture<String> source = new CompletableFuture<>();

        CompletableFuture<String> result = template.withTimeout(source, Duration.ofMillis(50));

        assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    @DisplayName("withFallback：source 失败时，应返回 fallback 值")
    void withFallback_should_return_fallback_value() throws Exception {
        CompletableFuture<String> source = new CompletableFuture<>();

        CompletableFuture<String> result = template.withFallback(source, error -> "FALLBACK");
        source.completeExceptionally(new IllegalStateException("failed"));

        assertThat(result.get(1, TimeUnit.SECONDS)).isEqualTo("FALLBACK");
    }
}
