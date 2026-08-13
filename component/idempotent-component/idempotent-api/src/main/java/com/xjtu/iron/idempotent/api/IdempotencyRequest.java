package com.xjtu.iron.idempotent.api;

/**
 * 普通幂等执行请求。
 *
 * <p>三个最容易混淆的字段：</p>
 * <ul>
 *     <li>{@code key}：判断“是不是同一个逻辑请求”的主键；</li>
 *     <li>{@code requestHash}：判断“同一个 key 的请求内容是不是同一件事”；</li>
 *     <li>{@code routeKey}：决定未来应该路由到哪个业务分片，不参与 Repository 选择。</li>
 * </ul>
 *
 * <p>当前组件本身不实现分库分表算法，只负责把 routeKey 完整传递并持久化，
 * 以便未来业务执行和 Reliable Task 恢复能够重新进入同一分片。</p>
 */
public final class IdempotencyRequest {

    private final String key;
    private final String requestHash;
    private final String routeKey;
    private final IdempotencyOptions options;

    private IdempotencyRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    /** 适合不需要 requestHash/routeKey 的简单调用。 */
    public static IdempotencyRequest of(String key, IdempotencyOptions options) {
        return builder().key(key).options(options).build();
    }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public IdempotencyOptions getOptions() { return options; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private IdempotencyOptions options;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder options(IdempotencyOptions value) { this.options = value; return this; }
        public IdempotencyRequest build() { return new IdempotencyRequest(this); }
    }
}
