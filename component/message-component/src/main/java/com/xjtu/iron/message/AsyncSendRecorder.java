package com.xjtu.iron.message;

import com.xjtu.iron.message.result.SendResult;

public interface AsyncSendRecorder {
    void onSuccess(Message<?> message);
    void onFailure(Message<?> message, SendResult result);
}

