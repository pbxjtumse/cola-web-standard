package com.xjtu.iron.message.api.spi;

import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendStatus;

/**
 * 表示具体 Provider 返回给 core 的标准化发送结果。
 *
 * @param nativeMessageId 中间件原生消息标识
 * @param status Provider 能够判断出的发送状态
 * @param failureType 标准失败分类
 * @param detail 诊断说明
 */
public record ProviderSendResult(
        String nativeMessageId,
        SendStatus status,
        SendFailureType failureType,
        String detail) {

    /**
     * 创建明确成功的 Provider 结果。
     *
     * @param nativeMessageId 原生消息标识
     * @return 明确成功结果
     */
    public static ProviderSendResult confirmed(String nativeMessageId) {
        // 成功结果没有失败分类。
        return new ProviderSendResult(
                nativeMessageId,
                SendStatus.CONFIRMED,
                SendFailureType.NONE,
                "Provider confirmed the message");
    }

    /**
     * 创建确定失败、拒绝或不确定结果。
     *
     * @param status 标准状态
     * @param failureType 失败分类
     * @param detail 诊断说明
     * @return 标准 Provider 结果
     */
    public static ProviderSendResult of(
            SendStatus status,
            SendFailureType failureType,
            String detail) {
        // 失败结果可能尚未获得原生消息标识。
        return new ProviderSendResult(null, status, failureType, detail);
    }
}
