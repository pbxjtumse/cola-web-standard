package com.xjtu.iron.idempotent.demo;

import com.xjtu.iron.idempotent.api.execution.*;
import com.xjtu.iron.idempotent.api.policy.*;
import com.xjtu.iron.idempotent.api.recovery.*;
import com.xjtu.iron.idempotent.api.state.*;
import com.xjtu.iron.idempotent.api.result.*;
import com.xjtu.iron.idempotent.api.spi.IdempotencyRequestHasher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * V1.3 Demo：
 * 1) WINDOWED + SNAPSHOT；
 * 2) DURABLE + Tx-A/Tx-B/Tx-C + SNAPSHOT；
 * 3) DURABLE + REFERENCE。
 */
@RestController
@RequestMapping("/demo/idempotency")
public class IdempotencyDemoController {

    private final IdempotencyExecutor executor;
    private final IdempotencyRequestHasher hasher;
    private final IdempotencySnapshotPolicyFactory snapshotPolicyFactory;
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyDemoController(
            IdempotencyExecutor executor,
            IdempotencyRequestHasher hasher,
            IdempotencySnapshotPolicyFactory snapshotPolicyFactory,
            JdbcTemplate jdbcTemplate) {
        this.executor = executor;
        this.hasher = hasher;
        this.snapshotPolicyFactory = snapshotPolicyFactory;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 有限幂等窗口：第一次返回值保存成 SNAPSHOT，窗口内重复请求返回同一份结果。
     */
    @PostMapping("/windowed/{key}")
    public IdempotencyResult<String> windowed(
            @PathVariable String key,
            @RequestParam(defaultValue = "payload") String payload) {

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key(key)
                .requestHash(hasher.hash(Map.of("payload", payload)))
                .routeKey("demo-windowed")
                .policyName("demo-windowed")
                .build();

        IdempotencyResultPolicy<String> snapshot =
                snapshotPolicyFactory.snapshot(
                        new IdempotencyTypeRef<String>() { });

        return executor.execute(
                request,
                snapshot,
                context -> "windowed executed, version="
                        + context.getVersion()
                        + ", payload=" + payload);
    }

    /**
     * 长期幂等 + JDBC 本地事务闭环。
     * SNAPSHOT 只是结果回放策略，不参与事务正确性的判定。
     */
    @PostMapping("/durable/{key}")
    public IdempotencyResult<String> durable(
            @PathVariable String key,
            @RequestParam(defaultValue = "payload") String payload,
            @RequestParam(defaultValue = "merchant-10001") String routeKey,
            @RequestParam(defaultValue = "false") boolean failAfterBusinessWrite) {

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key(key)
                .routeKey(routeKey)
                .requestHash(hasher.hash(
                        Map.of("payload", payload, "routeKey", routeKey)))
                .policyName("demo-durable")
                .build();

        IdempotencyResultPolicy<String> snapshot =
                snapshotPolicyFactory.snapshot(
                        new IdempotencyTypeRef<String>() { });

        return executor.execute(
                request,
                snapshot,
                context -> {
                    jdbcTemplate.update(
                            "INSERT INTO demo_business_record(idempotency_key,payload,created_at) VALUES (?,?,?)",
                            key,
                            payload,
                            Timestamp.from(Instant.now()));

                    if (failAfterBusinessWrite) {
                        throw new IllegalStateException(
                                "demo business failure after INSERT");
                    }

                    return "durable executed, generationVersion="
                            + context.getGenerationVersion()
                            + ", routeKey=" + context.getRouteKey()
                            + ", payload=" + payload;
                });
    }

    /**
     * REFERENCE 示例：幂等表只保存稳定引用 key，不长期保存业务 DTO JSON。
     * 重复请求时通过该引用重新读取真实业务数据。
     */
    @PostMapping("/durable-reference/{key}")
    public IdempotencyResult<String> durableReference(
            @PathVariable String key,
            @RequestParam(defaultValue = "payload") String payload) {

        IdempotencyRequest request = IdempotencyRequest.builder()
                .key(key)
                .routeKey("merchant-10001")
                .requestHash(hasher.hash(Map.of("payload", payload)))
                .policyName("demo-durable")
                .build();

        IdempotencyResultPolicy<String> reference =
                IdempotencyResultPolicies.reference(
                        new IdempotencyResultReference<>() {
                            @Override
                            public String capture(String value) {
                                // 这里只保存稳定业务引用，而不是把完整业务返回对象存进幂等表。
                                return key;
                            }

                            @Override
                            public String resolve(String referenceKey) {
                                String currentPayload = jdbcTemplate.queryForObject(
                                        "SELECT payload FROM demo_business_record "
                                                + "WHERE idempotency_key=? "
                                                + "ORDER BY id DESC LIMIT 1",
                                        String.class,
                                        referenceKey);
                                return "resolved by reference, key="
                                        + referenceKey
                                        + ", payload=" + currentPayload;
                            }
                        });

        return executor.execute(
                request,
                reference,
                context -> {
                    jdbcTemplate.update(
                            "INSERT INTO demo_business_record(idempotency_key,payload,created_at) VALUES (?,?,?)",
                            key,
                            payload,
                            Timestamp.from(Instant.now()));
                    return "created, key=" + key + ", payload=" + payload;
                });
    }

    @GetMapping("/durable/{key}/business-count")
    public Map<String, Object> durableBusinessCount(
            @PathVariable String key) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM demo_business_record WHERE idempotency_key=?",
                Integer.class,
                key);
        return Map.of(
                "key", key,
                "businessCount", count == null ? 0 : count);
    }
}
