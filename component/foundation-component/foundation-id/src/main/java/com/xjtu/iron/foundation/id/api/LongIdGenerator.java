package com.xjtu.iron.foundation.id.api;

/** 生成 long 类型技术标识，并避免调用方重复处理拆箱。 */
@FunctionalInterface
public interface LongIdGenerator extends IdGenerator<Long> {

    /**
     * 生成下一个原始 long 标识。
     *
     * @return long 标识
     */
    long nextLongId();

    @Override
    default Long nextId() {
        return nextLongId();
    }
}
