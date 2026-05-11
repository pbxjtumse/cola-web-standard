package com.xjtu.iron.message.handler;

import com.xjtu.iron.message.result.SendResult;
import com.xjtu.iron.message.Message;

/**
 * @author pbxjtu
 */
public interface SendFailureHandler {
    /**
     * @param message 消息体
     * @param result 结果
     */
    void onFailure(Message<?> message, SendResult result);
}
