package com.xjtu.iron.message.spi;

import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 表示 Provider 层返回给 core 的标准发送结果。
 *
 * @param status Provider 能够确认的发送状态
 * @param failureType 标准失败类型
 * @param providerMessageId Broker 或 Provider 原生消息 ID
 * @param description 诊断描述
 * @param metadata 原生诊断元数据
 */
public record ProviderSendResult(
        SendStatus status,
        SendFailureType failureType,
        String providerMessageId,
        String description,
        Map<String, String> metadata) {

    /**
     * 统一校验结果一致性并复制元数据。
     */
    public ProviderSendResult {
        // status 不能为空。
        status = Objects.requireNonNull(status, "status must not be null");
        // failureType 不能为空。
        failureType = Objects.requireNonNull(failureType, "failureType must not be null");
        // 成功确认时失败类型必须为 NONE。
        if (status == SendStatus.CONFIRMED && failureType != SendFailureType.NONE) {
            // 防止 Provider 返回互相矛盾的结果。
            throw new IllegalArgumentException("confirmed result must use NONE failureType");
        }
        // 非成功状态不能使用 NONE。
        if (status != SendStatus.CONFIRMED && failureType == SendFailureType.NONE) {
            // 失败原因缺失会降低治理可用性。
            throw new IllegalArgumentException("non-confirmed result must provide failureType");
        }
        // 元数据执行防御性复制。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /**
     * 创建明确成功结果。
     *
     * @param providerMessageId 原生消息 ID
     * @param metadata 原生元数据
     * @return 成功结果
     */
    public static ProviderSendResult confirmed(
            String providerMessageId,
            Map<String, String> metadata) {
        // 成功结果统一使用 NONE 失败类型。
        return new ProviderSendResult(
                SendStatus.CONFIRMED,
                SendFailureType.NONE,
                providerMessageId,
                null,
                metadata);
    }

    /**
     * 创建没有额外元数据的明确成功结果。
     *
     * @param providerMessageId 原生消息 ID
     * @return 成功结果
     */
    public static ProviderSendResult confirmed(String providerMessageId) {
        // 复用完整工厂方法。
        return confirmed(providerMessageId, Map.of());
    }

    /**
     * 创建非成功结果。
     *
     * @param status 状态
     * @param failureType 失败类型
     * @param description 诊断描述
     * @return 非成功结果
     */
    public static ProviderSendResult failed(
            SendStatus status,
            SendFailureType failureType,
            String description) {
        // 非成功工厂不设置原生消息 ID 和额外元数据。
        return new ProviderSendResult(
                status,
                failureType,
                null,
                description,
                Map.of());
    }
}
