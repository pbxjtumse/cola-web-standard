/**
 * 异常 generation 的恢复入口与策略。
 *
 * <p>这里只定义“允许恢复什么、如何发起恢复”；扫描、任务调度、分布式抢占属于外部 Reliable Task 体系。</p>
 */
package com.xjtu.iron.idempotent.api.recovery;
