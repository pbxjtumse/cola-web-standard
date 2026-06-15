package com.xjtu.iron.concurrency.api.error;


/**
 * 异步错误分类规则。
 *
 * <p>
 * 业务系统可以注册多条规则。
 * 最终由 CompositeAsyncErrorClassifier 按优先级逐一匹配。
 * </p>
 */
public interface AsyncErrorClassificationRule {

    /**
     * 判断当前规则是否支持该异常。
     *
     * @param context 错误分类上下文
     * @return 是否支持
     */
    boolean supports(AsyncErrorClassificationContext context);

    /**
     * 对异常进行分类。
     *
     * @param context 错误分类上下文
     * @return 结构化异步错误
     */
    AsyncError classify(AsyncErrorClassificationContext context);

    /**
     * 规则优先级。
     *
     * <p>
     * 数值越小，优先级越高。
     * </p>
     */
    default int order() {
        return 0;
    }
}
