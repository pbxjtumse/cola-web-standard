package com.xjtu.iron.message.spi;

import java.util.Map;

/**
 * Provider 原生消息位置抽象，用于日志和诊断。
 */
public interface ProviderMessagePosition {
    /**
     * 返回可放入 ConsumeContext attributes 的稳定字段。
     *
     * @return 位置属性
     */
    Map<String, String> attributes();
}
