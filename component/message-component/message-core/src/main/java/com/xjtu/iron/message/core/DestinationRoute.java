package com.xjtu.iron.message.core;

import com.xjtu.iron.message.api.MessageCategory;
import com.xjtu.iron.message.api.MessageDestination;
import com.xjtu.iron.message.spi.ProviderDestination;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示一个逻辑目的地到 Provider 物理目的地的精确路由。
 *
 * @param namespace 逻辑命名空间
 * @param name 逻辑名称
 * @param category 逻辑类别
 * @param providerName Provider 名称
 * @param physicalName 物理 Topic 或同等名称
 * @param attributes Provider 扩展路由属性
 */
public record DestinationRoute(
        String namespace,
        String name,
        MessageCategory category,
        String providerName,
        String physicalName,
        Map<String, String> attributes) {

    /**
     * 校验并复制路由配置。
     */
    public DestinationRoute {
        // 逻辑命名空间必须存在。
        namespace = requireText(namespace, "namespace");
        // 逻辑名称必须存在。
        name = requireText(name, "name");
        // 类别必须存在。
        category = Objects.requireNonNull(category, "category must not be null");
        // Provider 名称必须存在。
        providerName = requireText(providerName, "providerName").toLowerCase(java.util.Locale.ROOT);
        // 物理目的地必须存在。
        physicalName = requireText(physicalName, "physicalName");
        // 扩展属性执行防御性复制。
        attributes = attributes == null || attributes.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /**
     * 使用逻辑目的地创建路由。
     *
     * @param destination 逻辑目的地
     * @param providerName Provider 名称
     * @param physicalName 物理名称
     * @return 路由
     */
    public static DestinationRoute of(
            MessageDestination destination,
            String providerName,
            String physicalName) {
        // 省略 Provider 扩展属性时使用空 Map。
        return of(destination, providerName, physicalName, Map.of());
    }

    /**
     * 使用逻辑目的地和 Provider 属性创建路由。
     *
     * @param destination 逻辑目的地
     * @param providerName Provider 名称
     * @param physicalName 物理名称
     * @param attributes Provider 属性
     * @return 路由
     */
    public static DestinationRoute of(
            MessageDestination destination,
            String providerName,
            String physicalName,
            Map<String, String> attributes) {
        // 逻辑目的地不能为空。
        Objects.requireNonNull(destination, "destination must not be null");
        // providerHint 不属于路由身份，不复制到路由键中。
        return new DestinationRoute(
                destination.namespace(),
                destination.name(),
                destination.category(),
                providerName,
                physicalName,
                attributes);
    }

    /**
     * 判断路由是否匹配逻辑目的地。
     *
     * @param destination 逻辑目的地
     * @return 匹配时返回 true
     */
    public boolean matches(MessageDestination destination) {
        // 比较 namespace、name 和 category，忽略 providerHint。
        return namespace.equals(destination.namespace())
                && name.equals(destination.name())
                && category == destination.category();
    }

    /**
     * 转换为 Provider SPI 物理目的地。
     *
     * @return Provider 物理目的地
     */
    public ProviderDestination toProviderDestination() {
        // 直接复制已经校验过的 Provider 字段。
        return new ProviderDestination(providerName, physicalName, attributes);
    }

    /** 校验必填文本。 */
    private static String requireText(String value, String fieldName) {
        // null 或空白都属于非法配置。
        if (value == null || value.isBlank()) {
            // 指出具体非法字段。
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // 返回去除首尾空白的值。
        return value.trim();
    }
}
