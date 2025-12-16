package com.xjtu.iron.cola.web.enums;

/**
 * 发送状态
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
     * 不确定（超时 / 网络）
     */
    UNCERTAIN
}
