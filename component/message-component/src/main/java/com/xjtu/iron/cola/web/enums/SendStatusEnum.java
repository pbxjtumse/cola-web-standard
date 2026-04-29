package com.xjtu.iron.cola.web.enums;

/**
 * 发送状态 回答了发送的状态
 * @author pbxjtu
 */
public enum SendStatusEnum {
    /**
     * 明确成功
     */
    SUCCESS,
    /**
     * 明确失败
     */
    FAIL,
    /**
     * 无法确认：超时、网络断开、连接重置
     * 最危险，可能成功也可能失败
     */
    UNCERTAIN
}
