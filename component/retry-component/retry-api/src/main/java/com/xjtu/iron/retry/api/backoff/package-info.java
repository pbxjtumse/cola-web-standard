/**
 * 定义重试等待时间的计算协议、结果模型和内置退避策略。
 *
 * <p>退避策略只计算等待时间，真正的同步等待由 retry-core 执行。</p>
 */
package com.xjtu.iron.retry.api.backoff;
