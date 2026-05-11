package com.xjtu.iron.message.handler;

import com.xjtu.iron.message.Message;

/**
 *
 * @author pbxjtu
 */
public interface ConsumeFailureHandler {
    void onFailure(Message<?> message, Throwable exception, int retryCount);
}
