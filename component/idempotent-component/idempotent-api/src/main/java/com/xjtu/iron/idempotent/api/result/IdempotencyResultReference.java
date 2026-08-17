package com.xjtu.iron.idempotent.api.result;

/**
 * REFERENCE 结果策略使用的稳定业务引用。
 *
 * <p>例如第一次创建订单返回 OrderResult，capture 可以只保存 orderId；
 * 重复请求时 resolve(orderId) 再查询当前订单并组装返回值。</p>
 */
public interface IdempotencyResultReference<T> {

    String capture(T value) throws Exception;

    T resolve(String reference) throws Exception;
}
