package com.xjtu.iron;


import com.xjtu.iron.concurrency.core.rejection.CallerRunsRejectedExecutionHandler;
import com.xjtu.iron.concurrency.core.task.CallerRunsAware;
import com.xjtu.iron.concurrency.core.task.RejectedTaskAware;
import junit.framework.TestCase;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link CallerRunsRejectedExecutionHandler} 行为测试。
 */
public class CallerRunsRejectedExecutionHandlerTest extends TestCase {

    /**
     * 线程池仍然运行时，任务应先收到 CALLER_THREAD 标记，再在提交线程直接执行。
     */
    public void testRunsInCallerThreadAndMarksMode() {
        CallerRunsRejectedExecutionHandler handler = new CallerRunsRejectedExecutionHandler();
        ThreadPoolExecutor executor = newExecutor();
        RecordingTask task = new RecordingTask();
        String callerThread = Thread.currentThread().getName();

        try {
            handler.rejectedExecution(task, executor);

            assertTrue(task.callerThreadMarked.get());
            assertTrue(task.executed.get());
            assertEquals(callerThread, task.executionThread.get());
            assertFalse(task.rejected.get());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 线程池关闭后，CALLER_RUNS 不应继续执行新任务，而应显式通知拒绝并抛异常。
     */
    public void testShutdownExecutorRejectsInsteadOfRunning() {
        CallerRunsRejectedExecutionHandler handler = new CallerRunsRejectedExecutionHandler();
        ThreadPoolExecutor executor = newExecutor();
        RecordingTask task = new RecordingTask();
        executor.shutdownNow();

        try {
            handler.rejectedExecution(task, executor);
            fail("Expected RejectedExecutionException");
        } catch (RejectedExecutionException expected) {
            assertTrue(task.rejected.get());
            assertFalse(task.executed.get());
            assertFalse(task.callerThreadMarked.get());
        }
    }

    private ThreadPoolExecutor newExecutor() {
        return new ThreadPoolExecutor(
                1,
                1,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1)
        );
    }

    /**
     * 同时支持调用方线程执行感知和拒绝感知的测试任务。
     */
    private static final class RecordingTask
            implements Runnable, CallerRunsAware, RejectedTaskAware {

        private final AtomicBoolean callerThreadMarked = new AtomicBoolean(false);
        private final AtomicBoolean executed = new AtomicBoolean(false);
        private final AtomicBoolean rejected = new AtomicBoolean(false);
        private final AtomicReference<String> executionThread = new AtomicReference<>();

        @Override
        public void markCallerThreadExecution() {
            callerThreadMarked.set(true);
        }

        @Override
        public void reject(Throwable throwable) {
            rejected.set(true);
        }

        @Override
        public void run() {
            executed.set(true);
            executionThread.set(Thread.currentThread().getName());
        }
    }
}

