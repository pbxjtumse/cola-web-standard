package com.xjtu.iron.foundation.core.enumeration;

/**
 * 带稳定编码的枚举最小协议。
 *
 * @param <C> 编码类型，通常为 String、Integer 或 Long
 */
public interface CodeEnum<C> {

    C getCode();
}
