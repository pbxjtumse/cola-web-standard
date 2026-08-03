package com.xjtu.iron.foundation.resource;

/**
 * 根据资源位置创建资源对象。
 */
@FunctionalInterface
public interface ResourceLoader {

    Resource load(ResourceLocation location);
}
