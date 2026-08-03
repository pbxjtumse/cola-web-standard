package com.xjtu.iron.foundation.core.enumeration;

/**
 * 定义枚举解析失败时的处理策略。
 */
public enum UnknownEnumStrategy {
    /** 返回空结果。 */
    RETURN_EMPTY,
    /** 抛出参数异常。 */
    THROW_EXCEPTION
}
