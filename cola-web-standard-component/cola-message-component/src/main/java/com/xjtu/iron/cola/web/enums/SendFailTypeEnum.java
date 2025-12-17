package com.xjtu.iron.cola.web.enums;

/**
 * 失败的划分类型 当发生失败的时候  为什么
 * @author pbxjtu
 */
public enum SendFailTypeEnum {
    /**
     * 本地错误：序列化、线程池满、配置错误
     * 100% 没发出去
     */
    CLIENT_FAIL,
    /**
     * Broker 明确拒绝 权限、topic
     * 100% 没发进去
     */
    BROKER_REJECT,
    /**
     * 无法确认：超时、网络断开、连接重置
     * 最危险，可能成功也可能失败
     */
    UNCERTAIN
}
