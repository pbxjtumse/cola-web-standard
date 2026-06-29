package com.xjtu.iron.concurrency.core.boundary;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.event.TaskExecutionEvent;
import com.xjtu.iron.concurrency.api.listener.TaskExecutionListener;
import com.xjtu.iron.concurrency.api.task.TaskExecutionMode;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.api.task.TaskTimingSnapshot;
import com.xjtu.iron.concurrency.core.listener.CompositeTaskExecutionListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompositeTaskExecutionListener 边界测试")
class CompositeTaskExecutionListenerBoundaryTest {

    @Test
    @DisplayName("单个监听器抛异常时，不应影响后续监听器")
    void listener_exception_should_not_break_other_listeners() {
        AtomicInteger successCount = new AtomicInteger();

        TaskExecutionListener badListener = new TaskExecutionListener() {
            @Override
            public void onSuccess(TaskExecutionEvent event) {
                throw new IllegalStateException("listener error");
            }
        };
        TaskExecutionListener goodListener = new TaskExecutionListener() {
            @Override
            public void onSuccess(TaskExecutionEvent event) {
                successCount.incrementAndGet();
            }
        };

        CompositeTaskExecutionListener composite = new CompositeTaskExecutionListener(
                Arrays.asList(badListener, null, goodListener)
        );

        composite.onSuccess(successEvent());

        assertThat(successCount.get()).isEqualTo(1);
    }

    private static TaskExecutionEvent successEvent() {
        return new TaskExecutionEvent(
                new TaskMetadata("T1", "default", "task", null, null, null),
                AsyncTaskStatus.SUCCESS,
                TaskResultMode.RESULT_AWARE,
                TaskExecutionMode.THREAD_POOL,
                TaskTimingSnapshot.empty(),
                null,
                "success",
                Instant.now()
        );
    }
}
