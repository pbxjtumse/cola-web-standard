package com.xjtu.iron.cache.api;

@FunctionalInterface
public interface CacheLoader<T> {

    T load() throws Exception;
}