package com.xjtu.iron.idempotent.api.repository.recovery;

import java.time.Instant;

/**
 * 外部任务组件查询恢复候选项时使用的查询条件。
 *
 * <p>它只是 Repository 查询协议，不代表幂等组件自己拥有定时扫描线程。</p>
 */
public final class IdempotencyRecoveryQuery {

    private final String namespace;
    private final String routeKey;
    private final Instant now;
    private final int limit;

    public IdempotencyRecoveryQuery(String namespace, String routeKey, Instant now, int limit) {
        this.namespace = namespace;
        this.routeKey = routeKey;
        this.now = now;
        this.limit = limit;
    }

    public String getNamespace() { return namespace; }
    public String getRouteKey() { return routeKey; }
    public Instant getNow() { return now; }
    public int getLimit() { return limit; }
}
