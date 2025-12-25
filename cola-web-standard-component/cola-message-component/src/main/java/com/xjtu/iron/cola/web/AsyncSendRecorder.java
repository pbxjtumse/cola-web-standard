package com.xjtu.iron.cola.web;

import com.xjtu.iron.cola.web.result.SendResult;

public interface AsyncSendRecorder {
    void onSuccess(Message<?> message);
    void onFailure(Message<?> message, SendResult result);
}

