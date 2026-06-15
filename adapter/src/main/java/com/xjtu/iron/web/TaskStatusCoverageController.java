package com.xjtu.iron.web;


import com.xjtu.iron.concurrency.api.execution.executor.AsyncExecutor;
import com.xjtu.iron.concurrency.api.execution.registry.TaskExecutionRegistry;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;


@RestController
public class TaskStatusCoverageController {

    private final AsyncExecutor asyncExecutor;

    private final TaskExecutionRegistry taskExecutionRegistry;

    public TaskStatusCoverageController(AsyncExecutor asyncExecutor, TaskExecutionRegistry taskExecutionRegistry) {
        this.asyncExecutor = asyncExecutor;
        this.taskExecutionRegistry = taskExecutionRegistry;
    }

    @GetMapping("/demo/status/success")
    public Object success() {
        String value = asyncExecutor.submit(
                AsyncTask.of("default", "successTask", () -> "success").bizKey("STATUS_SUCCESS")).join();
        return result("SUCCESS", value);
    }

    @GetMapping("/demo/status/failed")
    public Object failed() {
        try {asyncExecutor.submit(AsyncTask.of("default", "failedTask", () -> {
                        throw new IllegalStateException("mock task failed");}).bizKey("STATUS_FAILED")).join();
            return result("FAILED", "unexpected success");
        } catch (Exception ex) {
            return result("FAILED", ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/demo/status/timeout")
    public Object timeout() {
        try {
            asyncExecutor.submit(
                    AsyncTask.of("default", "timeoutTask", () -> {sleep(1000);return "too slow";})
                            .timeout(Duration.ofMillis(100))
                            .bizKey("STATUS_TIMEOUT")).join();
            return result("TIMEOUT", "unexpected success");
        } catch (Exception ex) {
            return result("TIMEOUT", ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/demo/status/queue-timeout")
    public Object queueTimeout() {
        try {
            asyncExecutor.submit(AsyncTask.of("tiny-pool", "queueTimeoutTask", () -> {
                                sleep(500);
                                return "queue-timeout";})
                            .queueTimeout(Duration.ofMillis(10))
                            .bizKey("STATUS_QUEUE_TIMEOUT")
            ).join();

            return result("QUEUE_TIMEOUT", "unexpected success");
        } catch (Exception ex) {
            return result("QUEUE_TIMEOUT", ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/demo/status/fallback-success")
    public Object fallbackSuccess() {
        String value = asyncExecutor.submit(
                AsyncTask.of("default", "fallbackSuccessTask", () -> {
                            throw new IllegalStateException("origin failed");
                        })
                        .fallback(error -> "fallback-value")
                        .bizKey("STATUS_FALLBACK_SUCCESS")
        ).join().toString();

        return result("FALLBACK_SUCCESS", value);
    }

    @GetMapping("/demo/status/fallback-failed")
    public Object fallbackFailed() {
        try {
            asyncExecutor.submit(
                    AsyncTask.of("default", "fallbackFailedTask", () -> {
                                throw new IllegalStateException("origin failed");
                            })
                            .fallback(error -> {
                                throw new IllegalArgumentException("fallback also failed");
                            })
                            .bizKey("STATUS_FALLBACK_FAILED")
            ).join();

            return result("FALLBACK_FAILED", "unexpected success");
        } catch (Exception ex) {
            return result("FALLBACK_FAILED", ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/demo/status/rejected")
    public Object rejected() {
        try {
            for (int i = 0; i < 20; i++) {
                asyncExecutor.submit(
                        AsyncTask.of("tiny-pool", "rejectedTask-" + i, () -> {
                            sleep(3000);
                            return "ok";
                        }).bizKey("STATUS_REJECTED")
                );
            }

            return result("REJECTED", "submitted batch, check registry");
        } catch (Exception ex) {
            return result("REJECTED", ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    @GetMapping("/demo/status/recent")
    public Object recent() {
        return taskExecutionRegistry.recent(50);
    }

    private Map<String, Object> result(String status, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expectedStatus", status);
        result.put("value", value);
        result.put("recent", taskExecutionRegistry.recent(20));
        return result;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
