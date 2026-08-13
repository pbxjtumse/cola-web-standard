package com.xjtu.iron.idempotent.api;

/**
 * 外部 Reliable Task 调用 {@link IdempotencyExecutor#recover} 时使用的恢复请求。
 *
 * <p>{@code expectedOwnerToken / expectedVersion} 是防止“过时扫描任务”误接管的关键。
 * 扫描时看到 version=1，但任务真正执行时记录可能已经被别人恢复成 version=2，
 * 此时旧任务必须得到 STALE_RECOVERY_CANDIDATE，而不是再次执行业务。</p>
 */
public final class IdempotencyRecoveryRequest {

    /** 要恢复的逻辑幂等 Key。 */
    private final String key;

    /** 可选请求指纹；用于确认恢复任务仍对应同一业务语义。 */
    private final String requestHash;

    /** 分片路由元数据；Reliable Task 必须原样带回。 */
    private final String routeKey;

    /** 扫描候选时观察到的旧 owner；非空时参与 CAS 校验。 */
    private final String expectedOwnerToken;

    /** 扫描候选时观察到的旧 version；非空时参与 CAS 校验。 */
    private final Long expectedVersion;

    /** 恢复执行仍使用与正常执行一致的幂等策略。 */
    private final IdempotencyOptions options;

    private IdempotencyRecoveryRequest(Builder builder) {
        this.key = builder.key;
        this.requestHash = builder.requestHash;
        this.routeKey = builder.routeKey;
        this.expectedOwnerToken = builder.expectedOwnerToken;
        this.expectedVersion = builder.expectedVersion;
        this.options = builder.options;
    }

    public static Builder builder() { return new Builder(); }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getRouteKey() { return routeKey; }
    public String getExpectedOwnerToken() { return expectedOwnerToken; }
    public Long getExpectedVersion() { return expectedVersion; }
    public IdempotencyOptions getOptions() { return options; }

    public static final class Builder {
        private String key;
        private String requestHash;
        private String routeKey;
        private String expectedOwnerToken;
        private Long expectedVersion;
        private IdempotencyOptions options;

        public Builder key(String value) { this.key = value; return this; }
        public Builder requestHash(String value) { this.requestHash = value; return this; }
        public Builder routeKey(String value) { this.routeKey = value; return this; }
        public Builder expectedOwnerToken(String value) { this.expectedOwnerToken = value; return this; }
        public Builder expectedVersion(Long value) { this.expectedVersion = value; return this; }
        public Builder options(IdempotencyOptions value) { this.options = value; return this; }
        public IdempotencyRecoveryRequest build() { return new IdempotencyRecoveryRequest(this); }
    }
}
