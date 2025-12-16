package com.xjtu.iron.cola.web.enums;

/**
 * @author pbxjtu
 */
public enum SendFailTypeEnum {
    /**
     * 本地错误
     */
    CLIENT_FAIL,
    /**
     * Broker 明确拒绝
     */
    BROKER_REJECT,
    /**
     * 不确定是否成功（最危险）
     */
    UNCERTAIN
}
