/**
 * 幂等执行入口模型。
 *
 * <p>只描述一次调用如何进入/离开幂等组件：Request、Executor、Callback、Context、Result、Stage。
 * 不放 Repository、恢复策略和序列化实现。</p>
 */
package com.xjtu.iron.idempotent.api.execution;
