package com.xjtu.iron.cola.web.handler;

import com.xjtu.iron.cola.web.result.SendResult;
import com.xjtu.iron.cola.web.Message;

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
