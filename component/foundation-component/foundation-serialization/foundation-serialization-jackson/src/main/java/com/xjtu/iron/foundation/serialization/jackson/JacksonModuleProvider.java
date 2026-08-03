package com.xjtu.iron.foundation.serialization.jackson;

import com.fasterxml.jackson.databind.Module;

/**
 * 向 Foundation Jackson Mapper 提供显式模块。
 */
@FunctionalInterface
public interface JacksonModuleProvider {

    Module module();
}
