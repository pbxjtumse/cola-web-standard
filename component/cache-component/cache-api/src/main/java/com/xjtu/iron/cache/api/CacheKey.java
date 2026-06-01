package com.xjtu.iron.cache.api;


import java.util.Objects;
/**
 * 缓存 key 模型。
 * 1.不要在业务代码里直接拼接 Redis key。推荐 key 结构：
 *   namespace:cacheName:bizKey:version
 *   例如：
 *   marketing:campaignRule:campaignId:10001:v1
 * 2. 如果 Redis 配置了 key-prefix，例如 cache:，最终 Redis key 为：
 * cache:marketing:campaignRule:campaignId:10001:v1
 */
public final class CacheKey {

    /**
     * cacheName 缓存名称
     */
    private final String cacheName;
    /**
     * cacheName 命名空间
     */
    private final String namespace;
    /**
     * cacheName 业务键
     */
    private final String bizKey;
    /**
     * cacheName 版本号
     */
    private final String version;

    private CacheKey(String cacheName, String namespace, String bizKey, String version) {
        this.cacheName = requireText(cacheName, "cacheName");
        this.namespace = requireText(namespace, "namespace");
        this.bizKey = requireText(bizKey, "bizKey");
        this.version = requireText(version, "version");
    }

    public static CacheKey of(String cacheName, String namespace, String bizKey) {
        return new CacheKey(cacheName, namespace, bizKey, "v1");
    }

    public static CacheKey of(String cacheName, String namespace, String bizKey, String version) {
        return new CacheKey(cacheName, namespace, bizKey, version);
    }

    public String cacheName() {
        return cacheName;
    }

    public String namespace() {
        return namespace;
    }

    public String bizKey() {
        return bizKey;
    }

    public String version() {
        return version;
    }

    public String fullKey() {
        return namespace + ":" + cacheName + ":" + bizKey + ":" + version;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        String text = value.trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return text;
    }

    @Override
    public String toString() {
        return fullKey();
    }
}
