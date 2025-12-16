package com.xjtu.iron.cola.web;

import java.util.Map;

public interface Message<T> {

    /**
     *  全局唯一
     */
    String messageId();
    /**
     *  幂等、业务关联
     */
    String bizKey();
    /**
     *  业务数据
     */
    T payload();
    /**
     *  headers：不限定用途（traceId / tag / routingKey）
     */
    Map<String, String> headers();
    /**
     *  headers：不限定用途（traceId / tag / routingKey）
     */
    long bornTime();
}
