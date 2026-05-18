package com.xjtu.iron.cache.core;


import java.util.concurrent.Callable;

public interface CacheLoadGuard {

    <T> T execute(String key, Callable<T> loader) throws Exception;
}
