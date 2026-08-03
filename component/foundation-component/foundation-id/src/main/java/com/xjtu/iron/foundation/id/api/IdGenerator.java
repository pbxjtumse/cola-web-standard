package com.xjtu.iron.foundation.id.api;

/**
 * 生成指定类型的无业务语义标识。
 *
 * @param <T> 标识值类型
 */
@FunctionalInterface
public interface IdGenerator<T> {

    /**
     * 生成下一个标识。
     *
     * @return 新标识
     */
    T nextId();
}
