package com.xjtu.iron.idempotent.api.result;

/**
 * SUCCESS 后重复请求如何取得结果。
 */
public enum IdempotencyResultPolicyType {

    /** 只保存“已经成功”的事实，不保存业务返回值。 */
    NONE,

    /** 保存第一次成功返回值的快照；重复请求反序列化同一份快照。 */
    SNAPSHOT,

    /** 只保存稳定业务引用；重复请求根据引用重新解析当前业务结果。 */
    REFERENCE
}
