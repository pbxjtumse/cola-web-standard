package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import com.xjtu.iron.concurrency.api.retry.RetryPolicy;
import com.xjtu.iron.concurrency.core.testfixture.TestTaskFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase1 - TaskDefinition 快照语义测试")
class Phase1TaskDefinitionSnapshotAdditionalTest {

    @Test
    @DisplayName("from 应该复制基础 metadata，并通过便捷 getter 暴露")
    void fromShouldExposeMetadataConvenienceGetters() {
        AsyncTask<String> task = TestTaskFactory.task(
                "task-001",
                () -> "ok"
        );

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertSame(task.metadata(), definition.getMetadata());
        assertEquals("test-pool", definition.getExecutorName());
        assertEquals("test-task", definition.getTaskName());
        assertEquals("task-001", definition.getTaskId());
        assertEquals("biz-task-001", definition.getBizKey());
    }

    @Test
    @DisplayName("from 应该复制 operation 引用")
    void fromShouldCopyOperation() {
        Supplier<String> supplier = () -> "hello";

        AsyncTask<String> task = TestTaskFactory.task(
                "task-002",
                supplier
        );

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertSame(supplier, definition.getOperation());
        assertEquals("hello", definition.getOperation().get());
    }

    @Test
    @DisplayName("from 应该复制 timeout 和 queueTimeout 配置")
    void fromShouldCopyTimeoutSettings() {
        AsyncTask<String> task = TestTaskFactory.task(
                        "task-003",
                        () -> "ok"
                )
                .timeout(Duration.ofSeconds(3))
                .queueTimeout(Duration.ofMillis(500));

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertEquals(Duration.ofSeconds(3), definition.getTimeout());
        assertEquals(Duration.ofMillis(500), definition.getQueueTimeout());
    }

    @Test
    @DisplayName("from 应该复制 cancelOnTimeout 和 interruptOnTimeout 配置")
    void fromShouldCopyTimeoutCancelSettings() {
        AsyncTask<String> task = TestTaskFactory.task(
                        "task-004",
                        () -> "ok"
                )
                .cancelOnTimeout(true);

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertTrue(definition.isCancelOnTimeout());
    }

    @Test
    @DisplayName("from 应该复制 fallback 引用")
    void fromShouldCopyFallback() {
        Function<Throwable, String> fallback = error -> "fallback";

        AsyncTask<String> task = TestTaskFactory.task(
                        "task-005",
                        () -> {
                            throw new RuntimeException("boom");
                        }
                )
                .fallback(fallback);

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertSame(fallback, definition.getFallback());
        assertEquals("fallback", definition.getFallback().apply(new RuntimeException("x")));
    }

    @Test
    @DisplayName("from 应该复制 contextPropagation 配置")
    void fromShouldCopyContextPropagation() {
        AsyncTask<String> task = TestTaskFactory.task(
                        "task-006",
                        () -> "ok"
                )
                .contextPropagation(true);

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertTrue(definition.isContextPropagation());
    }

    @Test
    @DisplayName("from 应该复制 retryPolicy 引用")
    void fromShouldCopyRetryPolicy() {
        RetryPolicy retryPolicy = RetryPolicy.none();

        AsyncTask<String> task = TestTaskFactory.task(
                        "task-007",
                        () -> "ok"
                )
                .retryPolicy(retryPolicy);

        TaskDefinition<String> definition = TaskDefinition.from(task);

        assertSame(retryPolicy, definition.getRetryPolicy());
    }

    @Test
    @DisplayName("构造器中 retryPolicy 传 null 时应该兜底为 RetryPolicy.none")
    void constructorShouldUseNoneRetryPolicyWhenRetryPolicyIsNull() {
        AsyncTask<String> task = TestTaskFactory.task(
                "task-008",
                () -> "ok"
        );

        TaskDefinition<String> definition = new TaskDefinition<>(
                task.metadata(),
                task.getOperation(),
                task.getTimeout(),
                task.getQueueTimeout(),
                task.isCancelOnTimeout(),
                task.isInterruptOnTimeout(),
                task.getFallback(),
                task.isContextPropagation(),
                null
        );

        assertNotNull(definition.getRetryPolicy());
    }

    @Test
    @DisplayName("创建快照后修改 AsyncTask 的 timeout，不应影响 TaskDefinition")
    void modifyingAsyncTaskAfterSnapshotShouldNotAffectTimeout() {
        AsyncTask<String> task = TestTaskFactory.task(
                        "task-009",
                        () -> "ok"
                )
                .timeout(Duration.ofSeconds(1));

        TaskDefinition<String> definition = TaskDefinition.from(task);

        task.timeout(Duration.ofSeconds(99));

        assertEquals(Duration.ofSeconds(1), definition.getTimeout());
    }

    @Test
    @DisplayName("创建快照后修改 AsyncTask 的 queueTimeout，不应影响 TaskDefinition")
    void modifyingAsyncTaskAfterSnapshotShouldNotAffectQueueTimeout() {
        AsyncTask<String> task = TestTaskFactory.task(
                        "task-010",
                        () -> "ok"
                )
                .queueTimeout(Duration.ofMillis(100));

        TaskDefinition<String> definition = TaskDefinition.from(task);

        task.queueTimeout(Duration.ofMillis(999));

        assertEquals(Duration.ofMillis(100), definition.getQueueTimeout());
    }

    @Test
    @DisplayName("创建快照后修改 AsyncTask 的 fallback，不应影响 TaskDefinition")
    void modifyingAsyncTaskAfterSnapshotShouldNotAffectFallback() {
        Function<Throwable, String> oldFallback = error -> "old";
        Function<Throwable, String> newFallback = error -> "new";

        AsyncTask<String> task = TestTaskFactory.task(
                        "task-011",
                        () -> "ok"
                )
                .fallback(oldFallback);

        TaskDefinition<String> definition = TaskDefinition.from(task);

        task.fallback(newFallback);

        assertSame(oldFallback, definition.getFallback());
        assertEquals("old", definition.getFallback().apply(new RuntimeException("x")));
    }

    @Test
    @DisplayName("构造器应该保存 interruptOnTimeout 配置")
    void constructorShouldCopyInterruptOnTimeout() {
        AsyncTask<String> task = TestTaskFactory.task(
                "task-interrupt",
                () -> "ok"
        );

        TaskDefinition<String> definition = new TaskDefinition<>(
                task.metadata(),
                task.getOperation(),
                task.getTimeout(),
                task.getQueueTimeout(),
                task.isCancelOnTimeout(),
                true,
                task.getFallback(),
                task.isContextPropagation(),
                task.getRetryPolicy()
        );

        assertTrue(definition.isInterruptOnTimeout());
        assertTrue(definition.isInterruptOnCancel());
    }


    @Test
    @DisplayName("from 不允许传入 null")
    void fromShouldRejectNullTask() {
        assertThrows(
                NullPointerException.class,
                () -> TaskDefinition.from(null)
        );
    }

    @Test
    @DisplayName("构造器不允许 metadata 为 null")
    void constructorShouldRejectNullMetadata() {
        assertThrows(
                NullPointerException.class,
                () -> new TaskDefinition<>(
                        null,
                        () -> "ok",
                        null,
                        null,
                        false,
                        false,
                        null,
                        false,
                        RetryPolicy.none()
                )
        );
    }

    @Test
    @DisplayName("构造器不允许 operation 为 null")
    void constructorShouldRejectNullOperation() {
        AsyncTask<String> task = TestTaskFactory.task(
                "task-013",
                () -> "ok"
        );

        assertThrows(
                NullPointerException.class,
                () -> new TaskDefinition<>(
                        task.metadata(),
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        false,
                        RetryPolicy.none()
                )
        );
    }
}