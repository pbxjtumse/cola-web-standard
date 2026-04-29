package com.xjtu.iron.cola.web.execution;

import com.xjtu.iron.cola.web.Message;
import com.xjtu.iron.cola.web.result.SendResult;

/**
 * 不要在这里放任何模板方法。
 * 因为三种执行模式在“控制流”上是完全不同的：
 *  同步：阻塞等待
 *  异步：Future / Callback
 *  后台：持久化 + 立即返回
 * 它们不是一个算法的不同分支，而是不同算法
 * @author pbxjtu
 */
public interface SendExecution {
    SendResult execute(Message<?> message);
}
