package com.xjtu.iron.message.handler.impl;

import com.xjtu.iron.message.handler.SendFailureHandler;
import com.xjtu.iron.message.result.SendResult;
import com.xjtu.iron.message.Message;

/**
 * @author pbxjtu
 */
public class LogOnlySendFailureHandler implements SendFailureHandler {
    @Override
    public void onFailure(Message<?> message, SendResult result) {

    }
}
