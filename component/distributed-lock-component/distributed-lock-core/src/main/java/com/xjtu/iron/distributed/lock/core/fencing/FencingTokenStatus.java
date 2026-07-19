package com.xjtu.iron.distributed.lock.core.fencing;

/**
 * 独立 fencing token Provider 的执行状态。
 */
public enum FencingTokenStatus {

    /** 成功生成单调递增 token。 */
    ISSUED,

    /** Provider 不支持当前请求。 */
    NOT_SUPPORTED,

    /** Provider 访问数据库、Redis 或其他存储时发生异常。 */
    PROVIDER_ERROR
}
