package com.xjtu.iron.transaction.api.event;

/**
 * 事务事件监听器。
 *
 * <p>监听器属于观测扩展点，监听器自身异常不能反向改变业务事务结果。</p>
 */
@FunctionalInterface
public interface TransactionEventListener {

    void onEvent(TransactionEvent event);
}
