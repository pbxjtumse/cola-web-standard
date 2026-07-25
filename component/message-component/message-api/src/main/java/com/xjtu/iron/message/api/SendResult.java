package com.xjtu.iron.message.api;

import java.time.Instant;

/**
 * 表示发送流程标准化后的最终结果。
 *
 * @param messageId 统一消息标识
 * @param providerName 实际使用的 Provider
 * @param nativeMessageId 中间件返回的原生消息标识
 * @param status 最终确认状态
 * @param stage 流程结束或失败阶段
 * @param failureType 标准失败分类
 * @param detail 面向诊断的简短说明
 * @param completedAt 结果完成时间
 */
public record SendResult(
        String messageId,
        String providerName,
        String nativeMessageId,
        SendStatus status,
        SendStage stage,
        SendFailureType failureType,
        String detail,
        Instant completedAt) {

    /**
     * 判断消息是否已获得明确成功确认。
     *
     * @return 仅 CONFIRMED 返回 true
     */
    public boolean isConfirmed() {
        // UNKNOWN 不能当作成功，也不能武断地当作确定失败。
        return status == SendStatus.CONFIRMED;
    }
}
