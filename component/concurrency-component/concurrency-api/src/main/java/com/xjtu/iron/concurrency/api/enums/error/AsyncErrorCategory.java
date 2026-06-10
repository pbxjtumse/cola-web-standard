package com.xjtu.iron.concurrency.api.enums.error;


/**
 * 异步错误大类。
 *
 * <p>
 * 该枚举只表达错误所属的大方向，不表达具体原因和发生阶段。
 * </p>
 */
public enum AsyncErrorCategory {

    /**
     * 无错误。
     */
    NONE,

    /**
     * 应用侧异常。
     *
     * <p>
     * 例如业务异常、领域异常、应用异常、参数校验异常。
     * 并行组件不定义这些异常，只允许业务侧通过 AsyncErrorClassifier 映射进来。
     * </p>
     */
    APPLICATION,

    /**
     * 外部依赖异常。
     *
     * <p>
     * 例如 RPC、HTTP、DB、Redis、MQ、ES 等依赖异常。
     * </p>
     */
    DEPENDENCY,

    /**
     * 系统异常。
     *
     * <p>
     * 例如 NullPointerException、ClassCastException、IllegalStateException 等代码运行异常。
     * </p>
     */
    SYSTEM,

    /**
     * 并行组件自身异常。
     *
     * <p>
     * 例如线程池不存在、组件内部状态异常、配置错误等。
     * </p>
     */
    COMPONENT,

    /**
     * 资源异常。
     *
     * <p>
     * 例如线程池拒绝、队列满、资源不足、限流等。
     * </p>
     */
    RESOURCE,

    /**
     * 未知异常。
     */
    UNKNOWN
}
