package com.xjtu.iron.foundation.context;

import java.util.Map;

/**
 * 上下文外部载体，例如消息 Header、HTTP Header 或 Map。
 */
public interface ContextCarrier {

    String get(String name);

    void put(String name, String value);

    Map<String, String> asMap();
}
