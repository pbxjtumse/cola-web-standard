package com.xjtu.iron.transaction.api;

/**
 * 一期最小事务事件类型。
 */
public enum TransactionEventType {
    STARTED,
    COMPLETED,
    BUSINESS_FAILED,
    INFRASTRUCTURE_FAILED
}
