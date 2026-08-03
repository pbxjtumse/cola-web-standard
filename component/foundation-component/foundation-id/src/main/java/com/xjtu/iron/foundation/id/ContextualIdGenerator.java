package com.xjtu.iron.foundation.id;

/**
 * 定义能够根据命名空间和属性生成标识的协议。
 */
@FunctionalInterface
public interface ContextualIdGenerator {

    /** 根据生成上下文返回技术标识。 */
    String nextId(IdGenerationContext context);
}
