package com.xjtu.iron.concurrency.core.stress;

import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("stress")
@DisplayName("高并发提交压力测试")
class HighConcurrencySubmitStressTest {

    @Test
    @DisplayName("多调用方线程并发 submit：不应丢任务、不应出现竞态异常")
    void high_concurrency_submit_should_not_lose_tasks() throws Exception {
        int taskCount = 1000;
        ExecutorService callers = Executors.newFixedThreadPool(24);
        CountDownLatch done = new CountDownLatch(taskCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            for (int i = 0; i < taskCount; i++) {
                int index = i;
                callers.submit(() -> {
                    try {
                        AsyncTask<String> task = AsyncTask.of("default", "high-concurrency", () -> "OK")
                                .taskId("HC-" + index)
                                .timeout(Duration.ofSeconds(3));

                        String result = fixture.asyncExecutor().submit(task).get(3, TimeUnit.SECONDS);
                        if ("OK".equals(result)) {
                            success.incrementAndGet();
                        }
                    } catch (Throwable throwable) {
                        failed.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }

            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(success.get()).isEqualTo(taskCount);
            assertThat(failed.get()).isZero();
        } finally {
            callers.shutdownNow();
        }
    }
}
