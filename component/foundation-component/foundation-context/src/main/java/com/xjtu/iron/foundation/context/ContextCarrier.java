package com.xjtu.iron.foundation.context;

import java.util.Map;

/**
 * 定义跨边界传递字符串上下文的载体。
 *
 * <p>HTTP Header、消息 Header 和任务元数据都可以实现该协议。</p>
 */
public interface ContextCarrier {

    /** 写入一个上下文条目。 */
    void put(String name, String value);

    /** 读取一个上下文条目。 */
    String get(String name);

    /** 返回不可修改的全部条目。 */
    Map<String, String> entries();
}
