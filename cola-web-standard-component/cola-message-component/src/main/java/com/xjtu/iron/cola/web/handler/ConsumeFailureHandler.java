package com.xjtu.iron.cola.web.handler;

import com.xjtu.iron.cola.web.Message;

/**
 *
 * @author pbxjtu
 */
public interface ConsumeFailureHandler {
    void onFailure(Message<?> message, Throwable exception, int retryCount);
}
