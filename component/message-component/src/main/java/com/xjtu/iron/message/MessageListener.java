package com.xjtu.iron.message;

/**
 * @author pbxjtu
 */
public interface MessageListener<T> {

    void onMessage(Message<T> message);
}
