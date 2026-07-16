package com.xjtu.iron.distributed.lock.demo;

import com.xjtu.iron.distributed.lock.api.DistributedLockClient;
import com.xjtu.iron.distributed.lock.api.LockOptions;
import com.xjtu.iron.distributed.lock.api.LockResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo/distributed-lock")
public class DistributedLockDemoController {
    private final DistributedLockClient lockClient;
    public DistributedLockDemoController(DistributedLockClient lockClient) { this.lockClient = lockClient; }
    @GetMapping("/{bizKey}")
    public Map<String, Object> execute(@PathVariable String bizKey) {
        LockOptions options = LockOptions.builder()
                .namespace("demo")
                .leaseTime(Duration.ofSeconds(30))
                .waitTime(Duration.ofSeconds(2))
                .autoRenew(true)
                .maxRenewTime(Duration.ofMinutes(1))
                .build();
        LockResult<String> result = lockClient.execute("demo:job:" + bizKey, options, handle -> "processed:" + bizKey);
        return toBody(result);
    }

    @GetMapping("/default-options/{bizKey}")
    public Map<String, Object> executeWithConfiguredDefaults(@PathVariable String bizKey) {
        LockResult<String> result = lockClient.execute("demo:configured:job:" + bizKey,
                handle -> "processed-with-configured-defaults:" + bizKey);
        return toBody(result);
    }

    @GetMapping("/fencing/{bizKey}")
    public Map<String, Object> executeWithFencing(@PathVariable String bizKey) {
        LockOptions options = LockOptions.builder()
                .namespace("demo")
                .leaseTime(Duration.ofSeconds(30))
                .waitTime(Duration.ofSeconds(2))
                .autoRenew(true)
                .maxRenewTime(Duration.ofMinutes(1))
                .fencingRequired(true)
                .build();
        LockResult<String> result = lockClient.execute("demo:fencing:job:" + bizKey, options, handle -> {
            long fencingToken = handle.fencingToken()
                    .orElseThrow(() -> new IllegalStateException("fencing token is required but missing"));
            return "processed:" + bizKey + ", fencingToken=" + fencingToken;
        });
        return toBody(result);
    }

    private Map<String, Object> toBody(LockResult<String> result) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", result.status().name());
        body.put("stage", result.stage().name());
        body.put("acquired", result.acquired());
        body.put("value", result.value().orElse(null));
        body.put("ownerToken", result.ownerToken());
        body.put("fencingToken", result.fencingToken().orElse(null));
        return body;
    }
}
