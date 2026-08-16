package com.xjtu.iron.idempotent.demo;

import com.xjtu.iron.idempotent.api.*;
import com.xjtu.iron.idempotent.api.spi.IdempotencyRequestHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/** V1.1 普通执行 + 恢复执行示例。 */
@RestController
@RequestMapping("/demo/idempotency")
public class IdempotencyDemoController {

    private final IdempotencyExecutor executor;
    private final IdempotencyRequestHasher hasher;
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyDemoController(
            IdempotencyExecutor executor,
            IdempotencyRequestHasher hasher,
            JdbcTemplate jdbcTemplate) {
        this.executor = executor;
        this.hasher = hasher;
        this.jdbcTemplate = jdbcTemplate;
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
            @RequestParam(defaultValue = "merchant-10001") String routeKey,
            @RequestParam(defaultValue = "false") boolean failAfterBusinessWrite) {

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
                context -> {
                    // 这条 INSERT 是 Tx-B 的真实业务写。
                    // transaction-component 接入以后，它与 markSuccess 必须使用同一事务。
                    jdbcTemplate.update(
                            "INSERT INTO demo_business_record(idempotency_key,payload,created_at) VALUES (?,?,?)",
                            key,
                            payload,
                            Timestamp.from(Instant.now()));

                    if (failAfterBusinessWrite) {
                        // 用于验证：业务 SQL 已执行后抛异常，Tx-B 会回滚 INSERT，
                        // 随后 Tx-C 独立把幂等记录写成 FAILED。
                        throw new IllegalStateException("demo business failure after INSERT");
                    }

                    return "durable executed, fencingVersion=" + context.fencingVersion()
                            + ", routeKey=" + context.getRouteKey()
                            + ", payload=" + payload;
                });
    }

    @GetMapping("/durable/{key}/business-count")
    public Map<String, Object> durableBusinessCount(@PathVariable String key) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_business_record WHERE idempotency_key=?",
                Integer.class,
                key);
        return Map.of("key", key, "businessCount", count == null ? 0 : count);
    }
}
