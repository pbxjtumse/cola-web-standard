/**
 * 幂等策略模型。
 *
 * <p>描述一类业务长期稳定的幂等规则：WINDOWED/DURABLE、执行租约、窗口、锁策略等。
 * Policy 不承担单次请求身份，也不直接执行存储操作。</p>
 */
package com.xjtu.iron.idempotent.api.policy;
