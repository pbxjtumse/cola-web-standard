package com.xjtu.iron.foundation.id;

/**
 * 定义通用技术标识生成协议。
 *
 * @param <T> 标识类型
 */
@FunctionalInterface
public interface IdGenerator<T> {

    /** 生成下一个技术标识。 */
    T nextId();
}
