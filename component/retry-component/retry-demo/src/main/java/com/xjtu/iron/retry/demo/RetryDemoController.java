package com.xjtu.iron.retry.demo;

import com.xjtu.iron.retry.api.OperationSafety;
import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryPolicy;
import com.xjtu.iron.retry.api.RetryResult;
import com.xjtu.iron.retry.api.support.BackoffStrategies;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 展示异常重试、结果重试和不可重试三种核心场景。
 */
@RestController
@RequestMapping("/demo/retry")
public class RetryDemoController {

    private final RetryExecutor retryExecutor;

    public RetryDemoController(RetryExecutor retryExecutor) {
        this.retryExecutor = retryExecutor;
    }

    @GetMapping("/exception")
    public Map<String, Object> retryException(
            @RequestParam(defaultValue = "2") int failures) {
        AtomicInteger executions = new AtomicInteger();
        RetryResult<String> result = retryExecutor.execute(
                "demo-exception",
                context -> {
                    int current = executions.incrementAndGet();
                    if (current <= failures) {
                        throw new IOException("Simulated transient failure, execution=" + current);
                    }
                    return "SUCCESS";
                },
                "demo-exception"
        );
        return response(result, executions.get());
    }

    @GetMapping("/result")
    public Map<String, Object> retryResult(
            @RequestParam(defaultValue = "2") int pendingTimes) {
        AtomicInteger executions = new AtomicInteger();
        RetryPolicy policy = RetryPolicy.builder("demo-result")
                .maxAttempts(4)
                .maxDuration(Duration.ofSeconds(3))
                .operationSafety(OperationSafety.READ_ONLY)
                .retryIfResult("PENDING"::equals)
                .backoffStrategy(BackoffStrategies.fixed(Duration.ofMillis(100)))
                .build();

        RetryResult<String> result = retryExecutor.execute(
                "demo-result",
                context -> executions.incrementAndGet() <= pendingTimes ? "PENDING" : "SUCCESS",
                policy
        );
        return response(result, executions.get());
    }

    @GetMapping("/non-retryable")
    public Map<String, Object> nonRetryable() {
        AtomicInteger executions = new AtomicInteger();
        RetryResult<String> result = retryExecutor.execute(
                "demo-non-retryable",
                context -> {
                    executions.incrementAndGet();
                    throw new IllegalArgumentException("Simulated permanent failure");
                },
                "demo-exception"
        );
        return response(result, executions.get());
    }

    private Map<String, Object> response(RetryResult<?> result, int physicalExecutions) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("retryId", result.getRetryId());
        response.put("operationName", result.getOperationName());
        response.put("policyName", result.getPolicyName());
        response.put("status", result.getStatus());
        response.put("attempts", result.getAttempts());
        response.put("physicalExecutions", physicalExecutions);
        response.put("elapsedMillis", result.getElapsedTime().toMillis());
        response.put("value", result.getValue());
        response.put("failureType", result.getFailure() == null
                ? null
                : result.getFailure().getClass().getName());
        response.put("failureMessage", result.getFailure() == null
                ? null
                : result.getFailure().getMessage());
        return response;
    }
}
