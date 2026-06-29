package com.xjtu.iron.concurrency.core.stress;

import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.core.support.TestConcurrencyFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("stress")
@DisplayName("重复执行压力测试")
class RepeatedAsyncExecutionStressTest {

    @RepeatedTest(300)
    @DisplayName("重复 submit：不应出现状态污染或 Future 异常")
    void repeated_submit_should_always_return_success() throws Exception {
        try (TestConcurrencyFixture fixture = TestConcurrencyFixture.create()) {
            AsyncTask<String> task = AsyncTask.of("default", "repeated", () -> "OK")
                    .taskId("R-" + UUID.randomUUID())
                    .timeout(Duration.ofSeconds(2));

            String result = fixture.asyncExecutor().submit(task).get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("OK");
        }
    }
}
