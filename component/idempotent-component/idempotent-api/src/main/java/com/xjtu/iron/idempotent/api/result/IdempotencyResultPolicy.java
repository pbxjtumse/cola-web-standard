package com.xjtu.iron.idempotent.api.result;

/**
 * SUCCESS 结果的保存与重复请求回放策略。
 *
 * <p>它解决的是“第一次已经成功以后，第二次相同请求应该拿到什么”，
 * 不参与幂等唯一性、PROCESSING 抢占或 Recovery 正确性。</p>
 */
public interface IdempotencyResultPolicy<T> {

    IdempotencyResultPolicyType type();

    /**
     * 第一次成功时，把业务值转换成需要持久化到幂等记录里的策略载荷。
     * NONE 策略返回 null。
     */
    String capture(T value) throws Exception;

    /**
     * 重复请求发现 SUCCESS 时，把已经保存的策略载荷恢复成调用方需要的 T。
     * NONE 策略返回 null。
     */
    T replay(String storedValue) throws Exception;

    default boolean storesPayload() {
        return type() != IdempotencyResultPolicyType.NONE;
    }

    /**
     * SNAPSHOT/REFERENCE 如果没有历史 payload，通常无法按该策略回放。
     */
    default boolean requiresPayloadForReplay() {
        return storesPayload();
    }
}
