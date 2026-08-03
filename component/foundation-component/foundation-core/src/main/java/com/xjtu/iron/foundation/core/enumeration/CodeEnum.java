package com.xjtu.iron.foundation.core.enumeration;

/**
 * 定义具有稳定外部编码的枚举协议。
 *
 * @param <C> 编码类型
 */
public interface CodeEnum<C> {

    /** 返回稳定编码。 */
    C getCode();
}
