//package com.xjtu.iron.retry.core;
//
//import com.xjtu.iron.retry.api.RetryEvent;
//import com.xjtu.iron.retry.api.RetryEventType;
//import com.xjtu.iron.retry.api.RetryPolicy;
//import com.xjtu.iron.retry.api.RetryResult;
//import com.xjtu.iron.retry.api.RetryStatus;
//import com.xjtu.iron.retry.api.support.BackoffStrategies;
//import com.xjtu.iron.retry.core.time.RetrySleeper;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertInstanceOf;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//class DefaultRetryExecutorTest {
//
//    @AfterEach
//    void clearInterruptedFlag() {
//        Thread.interrupted();
//    }
//
//    @Test
//    void shouldRetryExceptionAndEventuallySucceed() {
//        AtomicInteger executions = new AtomicInteger();
//        RetryPolicy policy = RetryPolicy.builder("io-retry")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryOn(IOException.class)
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor();
//
//        RetryResult<String> result = executor.execute("query", context -> {
//            if (executions.incrementAndGet() < 3) {
//                throw new IOException("temporary failure");
//            }
//            return "OK";
//        }, policy);
//
//        assertTrue(result.isSuccess());
//        assertEquals(RetryStatus.SUCCESS, result.getStatus());
//        assertEquals("OK", result.getValue());
//        assertEquals(3, result.getAttempts());
//        assertEquals(3, executions.get());
//    }
//
//    @Test
//    void shouldStopWhenExceptionIsNotRetryable() {
//        RetryPolicy policy = RetryPolicy.builder("io-only")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryOn(IOException.class)
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor();
//
//        RetryResult<String> result = executor.execute("validate", context -> {
//            throw new IllegalArgumentException("invalid request");
//        }, policy);
//
//        assertEquals(RetryStatus.NOT_RETRYABLE, result.getStatus());
//        assertEquals(1, result.getAttempts());
//        assertInstanceOf(IllegalArgumentException.class, result.getFailure());
//    }
//
//    @Test
//    void shouldRetryByResultPredicate() {
//        AtomicInteger executions = new AtomicInteger();
//        RetryPolicy policy = RetryPolicy.builder("status-query")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryIfResult("PENDING"::equals)
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor();
//
//        RetryResult<String> result = executor.execute("status-query", context ->
//                executions.incrementAndGet() < 3 ? "PENDING" : "SUCCESS", policy);
//
//        assertEquals(RetryStatus.SUCCESS, result.getStatus());
//        assertEquals("SUCCESS", result.getValue());
//        assertEquals(3, result.getAttempts());
//    }
//
//    @Test
//    void shouldReturnExhaustedAfterMaximumAttempts() {
//        RetryPolicy policy = RetryPolicy.builder("always-fail")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryOn(IOException.class)
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor();
//
//        RetryResult<String> result = executor.execute("remote-call", context -> {
//            throw new IOException("still unavailable");
//        }, policy);
//
//        assertEquals(RetryStatus.EXHAUSTED, result.getStatus());
//        assertEquals(3, result.getAttempts());
//        assertTrue(result.isExhausted());
//    }
//
//    @Test
//    void shouldTimeoutBeforeBackoffExceedsRemainingDuration() {
//        AtomicInteger sleepCalls = new AtomicInteger();
//        RetrySleeper sleeper = duration -> sleepCalls.incrementAndGet();
//        RetryPolicy policy = RetryPolicy.builder("short-budget")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofMillis(10))
//                .retryOn(IOException.class)
//                .backoffStrategy(BackoffStrategies.fixed(Duration.ofMillis(50)))
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor(
//                new DefaultRetryPolicyRegistry(),
//                List.of(),
//                sleeper
//        );
//
//        RetryResult<String> result = executor.execute("remote-call", context -> {
//            throw new IOException("temporary failure");
//        }, policy);
//
//        assertEquals(RetryStatus.TIMED_OUT, result.getStatus());
//        assertEquals(1, result.getAttempts());
//        assertEquals(0, sleepCalls.get());
//    }
//
//    @Test
//    void shouldRestoreInterruptedFlagWhenBackoffIsInterrupted() {
//        RetrySleeper sleeper = duration -> {
//            throw new InterruptedException("interrupted while waiting");
//        };
//        RetryPolicy policy = RetryPolicy.builder("interruptible")
//                .maxAttempts(3)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryOn(IOException.class)
//                .backoffStrategy(BackoffStrategies.fixed(Duration.ofMillis(1)))
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor(
//                new DefaultRetryPolicyRegistry(),
//                List.of(),
//                sleeper
//        );
//
//        RetryResult<String> result = executor.execute("remote-call", context -> {
//            throw new IOException("temporary failure");
//        }, policy);
//
//        assertEquals(RetryStatus.INTERRUPTED, result.getStatus());
//        assertTrue(Thread.currentThread().isInterrupted());
//    }
//
//    @Test
//    void shouldPublishLifecycleEvents() {
//        List<RetryEvent> events = new ArrayList<>();
//        RetryPolicy policy = RetryPolicy.builder("event-policy")
//                .maxAttempts(2)
//                .maxDuration(Duration.ofSeconds(1))
//                .retryOn(IOException.class)
//                .build();
//        DefaultRetryExecutor executor = new DefaultRetryExecutor(
//                new DefaultRetryPolicyRegistry(),
//                List.of(events::add)
//        );
//
//        RetryResult<String> result = executor.execute("event-operation", context -> {
//            throw new IOException("failure");
//        }, policy);
//
//        assertEquals(RetryStatus.EXHAUSTED, result.getStatus());
//        assertFalse(events.isEmpty());
//        assertEquals(RetryEventType.EXECUTION_STARTED, events.getFirst().getEventType());
//        assertEquals(RetryEventType.EXHAUSTED, events.getLast().getEventType());
//        assertEquals(2L, events.stream()
//                .filter(event -> event.getEventType() == RetryEventType.ATTEMPT_STARTED)
//                .count());
//    }
//}
