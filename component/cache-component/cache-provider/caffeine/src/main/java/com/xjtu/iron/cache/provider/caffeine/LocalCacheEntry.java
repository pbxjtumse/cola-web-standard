package com.xjtu.iron.cache.provider.caffeine;

class LocalCacheEntry {

    private final Object value;
    private final boolean nullValue;
    private final long expireAtMillis;

    private LocalCacheEntry(Object value, boolean nullValue, long expireAtMillis) {
        this.value = value;
        this.nullValue = nullValue;
        this.expireAtMillis = expireAtMillis;
    }

    static LocalCacheEntry of(Object value, long expireAtMillis) {
        return new LocalCacheEntry(value, false, expireAtMillis);
    }

    static LocalCacheEntry nullValue(long expireAtMillis) {
        return new LocalCacheEntry(null, true, expireAtMillis);
    }

    boolean isExpired() {
        return System.currentTimeMillis() >= expireAtMillis;
    }

    Object getValue() {
        return value;
    }

    boolean isNullValue() {
        return nullValue;
    }
}
