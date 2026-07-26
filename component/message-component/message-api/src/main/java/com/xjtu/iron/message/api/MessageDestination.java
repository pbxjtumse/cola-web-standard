package com.xjtu.iron.message.api;

import java.util.Locale;
import java.util.Objects;

/**
 * 表示业务代码使用的逻辑消息目的地。
 *
 * <p>该对象不直接等价于 Kafka Topic、RocketMQ Topic 或 Pulsar Topic。
 * core 会通过 DestinationResolver 将它解析为具体 Provider 和物理目的地。</p>
 *
 * @param name 逻辑消息名称，例如 order-paid
 * @param namespace 业务命名空间或领域边界，例如 trade；不建议放 dev、sit、prod 环境名
 * @param category 消息业务类别
 * @param providerHint 可选 Provider 提示；为空时由路由表和默认 Provider 决定
 */
public record MessageDestination(
        String name,
        String namespace,
        MessageCategory category,
        String providerHint) {

    /**
     * 创建后统一校验并标准化字段。
     */
    public MessageDestination {
        // name 是逻辑路由键的一部分，必须存在。
        name = requireText(name, "name");
        // namespace 用于隔离不同领域中的同名消息，必须存在。
        namespace = requireText(namespace, "namespace");
        // category 不能缺失，否则无法表达消息业务语义。
        category = Objects.requireNonNull(category, "category must not be null");
        // providerHint 是可选字段，空白值统一转换为 null。
        providerHint = normalizeProvider(providerHint);
    }

    /**
     * 创建事件目的地。
     *
     * @param namespace 业务命名空间
     * @param name 逻辑名称
     * @return 事件目的地
     */
    public static MessageDestination event(String namespace, String name) {
        // Provider 留给路由配置决定。
        return new MessageDestination(name, namespace, MessageCategory.EVENT, null);
    }

    /**
     * 创建命令目的地。
     *
     * @param namespace 业务命名空间
     * @param name 逻辑名称
     * @return 命令目的地
     */
    public static MessageDestination command(String namespace, String name) {
        // Provider 留给路由配置决定。
        return new MessageDestination(name, namespace, MessageCategory.COMMAND, null);
    }

    /**
     * 创建通知目的地。
     *
     * @param namespace 业务命名空间
     * @param name 逻辑名称
     * @return 通知目的地
     */
    public static MessageDestination notification(String namespace, String name) {
        // Provider 留给路由配置决定。
        return new MessageDestination(name, namespace, MessageCategory.NOTIFICATION, null);
    }

    /**
     * 返回带 Provider 提示的新目的地。
     *
     * @param providerName Provider 名称
     * @return 新目的地
     */
    public MessageDestination withProviderHint(String providerName) {
        // record 不可变，因此通过新实例表达覆盖提示。
        return new MessageDestination(name, namespace, category, providerName);
    }

    /**
     * 返回用于日志、路由键和诊断的稳定逻辑名称。
     *
     * @return namespace:category:name 格式逻辑名称
     */
    public String qualifiedName() {
        // category 使用小写，便于配置文件和日志统一。
        return namespace + ":" + category.name().toLowerCase() + ":" + name;
    }

    /** 标准化可选文本。 */
    private static String normalize(String value) {
        // null 直接保持 null。
        if (value == null) {
            // 返回 null 表示未提供。
            return null;
        }
        // 去除首尾空白。
        String trimmed = value.trim();
        // 空白值转换为 null。
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 标准化可选 Provider 名称。 */
    private static String normalizeProvider(String value) {
        // 先使用通用可选文本规则。
        String normalized = normalize(value);
        // Provider 未指定时保持 null，否则统一使用小写稳定名称。
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    /** 校验必填文本。 */
    private static String requireText(String value, String fieldName) {
        // 使用统一标准化规则。
        String normalized = normalize(value);
        // 必填字段为空时拒绝创建目的地。
        if (normalized == null) {
            // 抛出参数异常并指出字段名。
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // 返回合法文本。
        return normalized;
    }
}
