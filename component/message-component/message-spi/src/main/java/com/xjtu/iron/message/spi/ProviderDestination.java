package com.xjtu.iron.message.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示逻辑目的地解析后的 Provider 物理目的地。
 *
 * @param providerName Provider 稳定名称
 * @param physicalName 物理 Topic 或同等目的地名称
 * @param attributes 路由配置中提供给 Provider 的扩展属性
 */
public record ProviderDestination(
        String providerName,
        String physicalName,
        Map<String, String> attributes) {

    /**
     * 校验并复制物理目的地配置。
     */
    public ProviderDestination {
        // Provider 名称必须存在。
        if (providerName == null || providerName.isBlank()) {
            // 没有 Provider 就无法完成发送或订阅。
            throw new IllegalArgumentException("providerName must not be blank");
        }
        // 去除 Provider 名称首尾空白。
        providerName = providerName.trim().toLowerCase(java.util.Locale.ROOT);
        // 物理目的地必须存在。
        if (physicalName == null || physicalName.isBlank()) {
            // 物理 Topic 为空时直接拒绝。
            throw new IllegalArgumentException("physicalName must not be blank");
        }
        // 去除物理名称首尾空白。
        physicalName = physicalName.trim();
        // 扩展属性执行防御性复制。
        attributes = attributes == null || attributes.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
