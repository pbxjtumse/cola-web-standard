package com.xjtu.iron.message.api;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表示统一的消息发送结果。
 *
 * @param messageId 组件消息 ID
 * @param destination 逻辑目的地
 * @param providerName 实际使用的 Provider
 * @param physicalDestination 实际物理目的地
 * @param status 发送状态
 * @param stage 结果产生阶段
 * @param failureType 标准失败类型
 * @param providerMessageId Provider 或 Broker 返回的原生消息 ID
 * @param description 诊断描述
 * @param startedAt 发送开始时间
 * @param completedAt 发送完成时间
 * @param metadata Provider 返回的只读诊断元数据
 */
public record SendResult(
        String messageId,
        MessageDestination destination,
        String providerName,
        String physicalDestination,
        SendStatus status,
        SendStage stage,
        SendFailureType failureType,
        String providerMessageId,
        String description,
        Instant startedAt,
        Instant completedAt,
        Map<String, String> metadata) {

    /**
     * 执行结果字段标准化和防御性复制。
     */
    public SendResult {
        // 没有元数据时统一使用空不可变 Map。
        metadata = metadata == null || metadata.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    /**
     * 判断消息是否已经获得明确成功确认。
     *
     * @return 已确认时返回 true
     */
    public boolean confirmed() {
        // 只有 CONFIRMED 才能被视为明确发送成功。
        return status == SendStatus.CONFIRMED;
    }

    /**
     * 判断结果是否不确定。
     *
     * @return 不确定时返回 true
     */
    public boolean unknown() {
        // UNKNOWN 不能等价为普通失败，也不能无条件重发。
        return status == SendStatus.UNKNOWN;
    }
}
