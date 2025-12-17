package com.xjtu.iron.cola.web.client;

public interface MqSendCallback {
    void onSuccess();

    void onFailure(Throwable ex);
}
