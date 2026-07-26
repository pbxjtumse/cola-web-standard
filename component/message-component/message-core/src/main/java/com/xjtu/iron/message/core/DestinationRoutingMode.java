package com.xjtu.iron.message.core;

/**
 * 定义未配置精确路由时的处理策略。
 */
public enum DestinationRoutingMode {

    /**
     * 严格模式：逻辑目的地必须配置精确路由，避免误发到自动生成的 Topic。
     */
    STRICT,

    /**
     * 隐式模式：允许使用默认 Provider 和标准化逻辑名称生成物理 Topic。
     *
     * <p>该模式适合本地开发和快速验证，不建议直接用于生产环境。</p>
     */
    IMPLICIT_DEFAULT
}
