package com.xjtu.iron.message.core;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * 表示 message-core 的稳定运行参数。
 *
 * @param defaultProviderName 未配置路由和 providerHint 时使用的默认 Provider
 * @param applicationName 默认消息来源；允许为空，此时 source 不会被强行伪造
 * @param defaultSchemaVersion 业务未声明版本时使用的默认结构版本
 * @param defaultConfirmTimeout 普通发送默认确认超时
 * @param routingMode 未配置精确路由时的处理策略
 * @param clock 生成消息时间和结果时间使用的统一时钟
 */
public record MessageComponentOptions(
        String defaultProviderName,
        String applicationName,
        String defaultSchemaVersion,
        Duration defaultConfirmTimeout,
        DestinationRoutingMode routingMode,
        Clock clock) {

    /**
     * 校验并标准化组件参数。
     */
    public MessageComponentOptions {
        // 默认 Provider 必须存在，否则无路由消息无法发送。
        defaultProviderName = requireText(defaultProviderName, "defaultProviderName").toLowerCase(java.util.Locale.ROOT);
        // applicationName 是可选值，空白统一转为 null。
        applicationName = normalize(applicationName);
        // 默认结构版本必须存在。
        defaultSchemaVersion = requireText(defaultSchemaVersion, "defaultSchemaVersion");
        // 默认确认超时不能为空。
        defaultConfirmTimeout = Objects.requireNonNull(
                defaultConfirmTimeout,
                "defaultConfirmTimeout must not be null");
        // 确认超时必须为正数。
        if (defaultConfirmTimeout.isZero() || defaultConfirmTimeout.isNegative()) {
            // 非正时间无法形成合理等待窗口。
            throw new IllegalArgumentException("defaultConfirmTimeout must be positive");
        }
        // 路由模式不能为空；生产默认应使用严格路由。
        routingMode = Objects.requireNonNull(routingMode, "routingMode must not be null");
        // 时钟不能为空，测试可传入固定时钟。
        clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 创建常用默认参数。
     *
     * @param defaultProviderName 默认 Provider
     * @param applicationName 应用名称
     * @return 默认参数
     */
    public static MessageComponentOptions defaults(
            String defaultProviderName,
            String applicationName) {
        // 默认使用结构版本 1、三秒确认超时和系统 UTC 时钟。
        return new MessageComponentOptions(
                defaultProviderName,
                applicationName,
                "1",
                Duration.ofSeconds(3),
                DestinationRoutingMode.STRICT,
                Clock.systemUTC());
    }

    /** 标准化可选文本。 */
    private static String normalize(String value) {
        // null 保持 null。
        if (value == null) {
            // 返回 null 表示未配置。
            return null;
        }
        // 去除首尾空白。
        String trimmed = value.trim();
        // 空白字符串转换为 null。
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 校验必填文本。 */
    private static String requireText(String value, String fieldName) {
        // 先执行统一标准化。
        String normalized = normalize(value);
        // 必填字段为空时立即失败。
        if (normalized == null) {
            // 明确指出非法字段。
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // 返回合法值。
        return normalized;
    }
}
