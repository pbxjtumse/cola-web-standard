package com.xjtu.iron.cola.web;

/**
 * @author pbxjtu
 */
public interface MessageListener<T> {

    void onMessage(Message<T> message);
}
