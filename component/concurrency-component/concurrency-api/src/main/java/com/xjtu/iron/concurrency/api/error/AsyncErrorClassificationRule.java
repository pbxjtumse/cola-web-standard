package com.xjtu.iron.concurrency.api.error;

/**
 * 异步错误分类规则。
 *
 * <p>
 * 业务系统可以注册多条规则，例如领域异常规则、RPC 异常规则、数据库异常规则。
 * 组合分类器会按 {@link #order()} 从小到大依次匹配，第一条支持当前异常的规则负责生成 AsyncError。
 * </p>
 */
public interface AsyncErrorClassificationRule {

    /**
     * 判断当前规则是否支持该异常。
     *
     * @param context 错误分类上下文
     * @return true 表示由当前规则处理
     */
    boolean supports(AsyncErrorClassificationContext context);

    /**
     * 把当前异常转换为结构化 AsyncError。
     *
     * @param context 错误分类上下文
     * @return 结构化异步错误，不应返回 null
     */
    AsyncError classify(AsyncErrorClassificationContext context);

    /**
     * 规则优先级。
     *
     * <p>数值越小，匹配优先级越高。</p>
     *
     * @return 规则顺序
     */
    default int order() {
        return 0;
    }
}
