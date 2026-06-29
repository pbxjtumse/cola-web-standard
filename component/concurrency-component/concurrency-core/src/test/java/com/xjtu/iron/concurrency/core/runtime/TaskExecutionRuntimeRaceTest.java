package com.xjtu.iron.concurrency.core.runtime;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.core.task.TaskExecutionRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaskExecutionRuntime 并发竞争测试")
class TaskExecutionRuntimeRaceTest {

    @RepeatedTest(100)
    @DisplayName("SUCCESS / TIMEOUT / FAILED 并发竞争原始结果时，只能有一个赢家")
    void only_one_base_outcome_should_win_under_race() throws Exception {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(60);
        AtomicInteger winners = new AtomicInteger();
        List<AsyncTaskStatus> statuses = List.of(
                AsyncTaskStatus.SUCCESS,
                AsyncTaskStatus.TIMEOUT,
                AsyncTaskStatus.FAILED
        );

        for (int i = 0; i < 60; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (runtime.tryResolveBaseOutcome(statuses.get(index % statuses.size()))) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();

        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(runtime.getStatus()).isIn(
                AsyncTaskStatus.SUCCESS,
                AsyncTaskStatus.TIMEOUT,
                AsyncTaskStatus.FAILED
        );
        pool.shutdownNow();
    }

    @RepeatedTest(100)
    @DisplayName("SUCCESS / FALLBACK_SUCCESS / CANCELLED 并发竞争最终结果时，只能有一个赢家")
    void only_one_final_outcome_should_win_under_race() throws Exception {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);
        ExecutorService pool = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(60);
        AtomicInteger winners = new AtomicInteger();
        List<AsyncTaskStatus> statuses = List.of(
                AsyncTaskStatus.SUCCESS,
                AsyncTaskStatus.FALLBACK_SUCCESS,
                AsyncTaskStatus.FALLBACK_FAILED
        );

        for (int i = 0; i < 60; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (runtime.tryFinalize(statuses.get(index % statuses.size()))) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();

        assertThat(done.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(runtime.getStatus()).isIn(
                AsyncTaskStatus.SUCCESS,
                AsyncTaskStatus.FALLBACK_SUCCESS,
                AsyncTaskStatus.FALLBACK_FAILED
        );
        pool.shutdownNow();
    }

    @Test
    @DisplayName("取消和成功并发竞争时，不应出现两个终态同时成功")
    void cancel_and_success_should_not_both_win() throws Exception {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();

        pool.submit(() -> {
            await(start);
            if (runtime.tryCancel()) {
                winners.incrementAndGet();
            }
        });
        pool.submit(() -> {
            await(start);
            if (runtime.tryResolveBaseOutcome(AsyncTaskStatus.SUCCESS)) {
                runtime.tryFinalize(AsyncTaskStatus.SUCCESS);
                winners.incrementAndGet();
            }
        });

        start.countDown();
        pool.shutdown();

        assertThat(pool.awaitTermination(3, TimeUnit.SECONDS)).isTrue();
        assertThat(winners.get()).isEqualTo(1);
        assertThat(runtime.getStatus()).isIn(AsyncTaskStatus.CANCELLED, AsyncTaskStatus.SUCCESS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
