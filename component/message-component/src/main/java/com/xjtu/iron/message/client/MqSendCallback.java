package com.xjtu.iron.message.client;

public interface MqSendCallback {
    void onSuccess();

    void onFailure(Throwable ex);
}
