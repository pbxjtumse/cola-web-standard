package com.xjtu.iron;


import com.xjtu.iron.concurrency.api.task.TaskExecutionMode;
import com.xjtu.iron.concurrency.api.task.TaskResultMode;
import com.xjtu.iron.concurrency.core.task.TaskExecutionRuntime;
import junit.framework.TestCase;

/**
 * {@link TaskExecutionRuntime} 执行方式测试。
 */
public class TaskExecutionRuntimeTest extends TestCase {

    /**
     * CALLER_RUNS 在进入 run() 之前标记 CALLER_THREAD，随后 tryMarkRunning 不应覆盖该模式。
     */
    public void testCallerThreadModeIsPreservedWhenRunning() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        runtime.markSubmitted();
        runtime.markCallerThreadExecution();

        assertEquals(TaskExecutionMode.CALLER_THREAD, runtime.getExecutionMode());
        assertTrue(runtime.tryMarkRunning());
        assertEquals(TaskExecutionMode.CALLER_THREAD, runtime.getExecutionMode());
    }

    /**
     * 未被 CALLER_RUNS 标记的普通任务，在真正运行时默认记录为 THREAD_POOL。
     */
    public void testPoolThreadModeIsAssignedWhenRunning() {
        TaskExecutionRuntime runtime = new TaskExecutionRuntime(TaskResultMode.RESULT_AWARE);

        runtime.markSubmitted();

        assertEquals(TaskExecutionMode.UNASSIGNED, runtime.getExecutionMode());
        assertTrue(runtime.tryMarkRunning());
        assertEquals(TaskExecutionMode.THREAD_POOL, runtime.getExecutionMode());
    }
}

