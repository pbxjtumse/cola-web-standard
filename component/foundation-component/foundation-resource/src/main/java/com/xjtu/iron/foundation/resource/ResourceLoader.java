package com.xjtu.iron.foundation.resource;

/**
 * 资源加载器。
 */
@FunctionalInterface
public interface ResourceLoader {

    Resource load(String location);
}
