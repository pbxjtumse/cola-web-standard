package com.xjtu.iron.cola.web.handler.impl;

import com.xjtu.iron.cola.web.handler.SendFailureHandler;
import com.xjtu.iron.cola.web.result.SendResult;
import com.xjtu.iron.cola.web.Message;

/**
 * 发送失败
 */
public class OutboxFailureHandler implements SendFailureHandler {
    @Override
    public void onFailure(Message<?> message, SendResult result) {

    }
}
