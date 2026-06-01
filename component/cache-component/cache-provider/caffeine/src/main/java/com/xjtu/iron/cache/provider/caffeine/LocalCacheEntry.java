package com.xjtu.iron.cache.provider.caffeine;

/**
 * Caffeine 本地缓存条目。
 *
 * <p>一期没有直接使用 Caffeine 的普通 expireAfterWrite，而是在 entry 中保存 expireAtMillis。
 * 这样可以支持每个 key 独立 TTL、空值 TTL 和 TTL 抖动。</p>
 */
class LocalCacheEntry {

    /** 缓存真实值。空值占位时为 null。 */
    private final Object value;

    /** 是否为空值占位。 */
    private final boolean nullValue;

    /** 绝对过期时间戳，单位毫秒。 */
    private final long expireAtMillis;

    /** 私有构造方法。 */
    private LocalCacheEntry(Object value, boolean nullValue, long expireAtMillis) {
        this.value = value;
        this.nullValue = nullValue;
        this.expireAtMillis = expireAtMillis;
    }

    /** 创建正常值条目。 */
    static LocalCacheEntry of(Object value, long expireAtMillis) {
        return new LocalCacheEntry(value, false, expireAtMillis);
    }

    /** 创建空值占位条目。 */
    static LocalCacheEntry nullValue(long expireAtMillis) {
        return new LocalCacheEntry(null, true, expireAtMillis);
    }

    /** 判断当前条目是否已经过期。 */
    boolean isExpired() {
        return System.currentTimeMillis() >= expireAtMillis;
    }

    /** 返回真实值。 */
    Object getValue() {
        return value;
    }

    /** 返回是否为空值占位。 */
    boolean isNullValue() {
        return nullValue;
    }
}
