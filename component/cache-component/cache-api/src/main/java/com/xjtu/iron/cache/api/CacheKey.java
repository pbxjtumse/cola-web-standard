package com.xjtu.iron.cache.api;


import java.util.Objects;

public final class CacheKey {

    private final String cacheName;
    private final String namespace;
    private final String bizKey;
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
