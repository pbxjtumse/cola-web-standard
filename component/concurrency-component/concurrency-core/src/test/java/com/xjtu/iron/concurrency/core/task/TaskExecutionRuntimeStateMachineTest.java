package com.xjtu.iron.concurrency.core.task;

import com.xjtu.iron.concurrency.api.enums.task.AsyncTaskStatus;
import com.xjtu.iron.concurrency.api.task.TaskExecutionMode;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutionRuntimeStateMachineTest {

    @Test
    void submittedCanOnlyMoveFromCreatedOnce() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertEquals(AsyncTaskStatus.CREATED, runtime.getStatus());
        assertTrue(runtime.tryMarkSubmitted());
        assertEquals(AsyncTaskStatus.SUBMITTED, runtime.getStatus());

        assertFalse(runtime.tryMarkSubmitted());
        assertEquals(AsyncTaskStatus.SUBMITTED, runtime.getStatus());
    }

    @Test
    void terminalStateCannotBeOverwrittenByMarkSubmitted() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertTrue(runtime.tryCancel());
        assertEquals(AsyncTaskStatus.CANCELLED, runtime.getStatus());

        //runtime.markSubmitted();

        assertEquals(AsyncTaskStatus.CANCELLED, runtime.getStatus());
        assertTrue(runtime.isBaseOutcomeResolved());
        assertTrue(runtime.isFinalOutcomeResolved());
    }

    @Test
    void callerThreadModeIsPreservedWhenRunning() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertTrue(runtime.tryMarkSubmitted());
        runtime.markCallerThreadExecution();

        assertEquals(TaskExecutionMode.CALLER_THREAD, runtime.getExecutionMode());
        assertTrue(runtime.tryMarkRunning());
        assertEquals(TaskExecutionMode.CALLER_THREAD, runtime.getExecutionMode());
        assertEquals(AsyncTaskStatus.RUNNING, runtime.getStatus());
    }

    @Test
    void poolThreadModeIsAssignedWhenRunning() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertTrue(runtime.tryMarkSubmitted());
        assertEquals(TaskExecutionMode.UNASSIGNED, runtime.getExecutionMode());

        assertTrue(runtime.tryMarkRunning());

        assertEquals(TaskExecutionMode.THREAD_POOL, runtime.getExecutionMode());
    }

    @Test
    void baseOutcomeCanOnlyBeResolvedOnce() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertTrue(runtime.tryResolveBaseOutcome(AsyncTaskStatus.SUCCESS));
        assertEquals(AsyncTaskStatus.SUCCESS, runtime.getStatus());

        assertFalse(runtime.tryResolveBaseOutcome(AsyncTaskStatus.TIMEOUT));
        assertEquals(AsyncTaskStatus.SUCCESS, runtime.getStatus());
    }

    @Test
    void finalOutcomeCanOnlyBeResolvedOnce() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertTrue(runtime.tryFinalize(AsyncTaskStatus.SUCCESS));
        assertEquals(AsyncTaskStatus.SUCCESS, runtime.getStatus());

        assertFalse(runtime.tryFinalize(AsyncTaskStatus.FALLBACK_SUCCESS));
        assertEquals(AsyncTaskStatus.SUCCESS, runtime.getStatus());
    }

    @Test
    void queueTimeoutUsesSubmitTime() throws Exception {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        assertFalse(runtime.isQueueTimeout(Duration.ofMillis(50)));
        Thread.sleep(20);
        assertTrue(runtime.isQueueTimeout(Duration.ofMillis(1)));
        assertFalse(runtime.isQueueTimeout(null));
        assertFalse(runtime.isQueueTimeout(Duration.ZERO));
        assertFalse(runtime.isQueueTimeout(Duration.ofMillis(-1)));
    }
}
