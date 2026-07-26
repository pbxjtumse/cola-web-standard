package com.xjtu.iron.message.api;

import java.time.Duration;

/**
 * 表示一次普通消息发送的调用选项。
 *
 * @param confirmTimeout 等待发送确认的最大时间；为空时使用组件默认值
 */
public record SendOptions(Duration confirmTimeout) {

    /**
     * 校验显式超时时间。
     */
    public SendOptions {
        // null 表示使用组件默认值。
        if (confirmTimeout != null && (confirmTimeout.isZero() || confirmTimeout.isNegative())) {
            // 非正超时没有可执行意义，因此直接拒绝。
            throw new IllegalArgumentException("confirmTimeout must be positive");
        }
    }

    /**
     * 返回使用组件默认值的发送选项。
     *
     * @return 默认发送选项
     */
    public static SendOptions defaults() {
        // null 超时由 core 解析为 MessageComponentOptions 中的默认值。
        return new SendOptions(null);
    }

    /**
     * 创建显式确认超时选项。
     *
     * @param timeout 确认超时
     * @return 发送选项
     */
    public static SendOptions withConfirmTimeout(Duration timeout) {
        // 统一通过 record 构造器执行合法性校验。
        return new SendOptions(timeout);
    }
}
