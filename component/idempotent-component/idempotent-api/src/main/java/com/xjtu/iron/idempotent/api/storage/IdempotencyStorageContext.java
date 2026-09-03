package com.xjtu.iron.idempotent.api.storage;

/**
 * 幂等记录的逻辑存储与分片路由上下文。
 *
 * <p>三个字段职责严格分离：</p>
 * <ul>
 *     <li>{@code storeName}：逻辑 Store 名称，例如 message-consume / payment；不等于 jdbc / redis 这类 Provider 名称。</li>
 *     <li>{@code shardKey}：在线点查和写入的稳定路由键，未来由 Sharded JDBC Provider 映射到具体库表。</li>
 *     <li>{@code scanBucket}：Reliable Recovery 扫描桶；它是固定逻辑桶，不代表物理表号。</li>
 * </ul>
 *
 * <p>V2 当前仍可以全部落到 JDBC 单表，但从 API 开始不再假设幂等数据永远只有一张物理表。</p>
 */
public final class IdempotencyStorageContext {

    public static final String DEFAULT_STORE_NAME = "default";

    private final String storeName;
    private final long shardKey;
    private final int scanBucket;

    public IdempotencyStorageContext(String storeName, long shardKey, int scanBucket) {
        this.storeName = requireText(storeName, "storeName must not be blank");
        if (scanBucket < 0) {
            throw new IllegalArgumentException("scanBucket must not be negative");
        }
        this.shardKey = shardKey;
        this.scanBucket = scanBucket;
    }

    public static IdempotencyStorageContext of(String storeName, long shardKey, int scanBucket) {
        return new IdempotencyStorageContext(storeName, shardKey, scanBucket);
    }

    public String getStoreName() { return storeName; }
    public long getShardKey() { return shardKey; }
    public int getScanBucket() { return scanBucket; }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
