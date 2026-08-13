package com.xjtu.iron.idempotent.demo;

import com.xjtu.iron.idempotent.api.*;
import com.xjtu.iron.idempotent.api.spi.IdempotencyRequestHasher;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/** V1.1 普通执行 + 恢复执行示例。 */
@RestController
@RequestMapping("/demo/idempotency")
public class IdempotencyDemoController {

    private final IdempotencyExecutor executor;
    private final IdempotencyRequestHasher hasher;

    public IdempotencyDemoController(
            IdempotencyExecutor executor,
            IdempotencyRequestHasher hasher) {
        this.executor = executor;
        this.hasher = hasher;
    }

    @PostMapping("/short-term/{key}")
    public IdempotencyResult<String> shortTerm(
            @PathVariable String key,
            @RequestParam(defaultValue = "payload") String payload) {

        IdempotencyOptions options = IdempotencyOptions.builder()
                .mode(IdempotencyMode.SHORT_TERM)
                .processingTimeout(Duration.ofSeconds(10))
                .idempotencyWindow(Duration.ofMinutes(5))
                .windowPolicy(IdempotencyWindowPolicy.FIXED_FROM_FIRST_ACQUIRE)
                .recordRetentionTtl(Duration.ofMinutes(1))
                .recoveryMode(IdempotencyRecoveryMode.NONE)
                .storeResult(true)
                .build();

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key(key)
                .requestHash(hasher.hash(Map.of("payload", payload)))
                .routeKey("demo-short-term")
                .options(options)
                .build();

        return executor.execute(
                request,
                String.class,
                context -> "short-term executed, version=" + context.getVersion()
                        + ", payload=" + payload);
    }

    @PostMapping("/durable/{key}")
    public IdempotencyResult<String> durable(
            @PathVariable String key,
            @RequestParam(defaultValue = "payload") String payload,
            @RequestParam(defaultValue = "merchant-10001") String routeKey) {

        IdempotencyOptions options = IdempotencyOptions.builder()
                .mode(IdempotencyMode.DURABLE)
                .processingTimeout(Duration.ofSeconds(30))
                .recoveryMode(IdempotencyRecoveryMode.EXTERNAL_TASK)
                .recoverFailed(true)
                .storeResult(true)
                .build();

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key(key)
                .routeKey(routeKey)
                .requestHash(hasher.hash(Map.of("payload", payload, "routeKey", routeKey)))
                .options(options)
                .build();

        return executor.execute(
                request,
                String.class,
                context -> "durable executed, fencingVersion=" + context.fencingVersion()
                        + ", routeKey=" + context.getRouteKey()
                        + ", payload=" + payload);
    }
}
