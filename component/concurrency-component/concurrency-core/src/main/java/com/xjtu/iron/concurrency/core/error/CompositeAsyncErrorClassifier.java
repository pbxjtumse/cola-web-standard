package com.xjtu.iron.concurrency.core.error;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationContext;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationRule;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.error.CompletableFutureExceptionUtils;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 组合异步错误分类器。
 *
 * <p>
 * Composite 可以理解为“把很多条小规则组合成一个统一分类器”。
 * TaskCommand 只依赖一个 AsyncErrorClassifier，但业务系统可以注册任意数量的
 * AsyncErrorClassificationRule，例如领域异常、RPC、数据库、缓存异常规则。
 * </p>
 *
 * <p>
 * 执行顺序如下：先按 order 从小到大遍历规则；第一条 supports 返回 true 的规则负责分类；
 * 如果没有任何规则匹配，则交给 fallbackClassifier 处理通用技术异常。
 * </p>
 */
public final class CompositeAsyncErrorClassifier implements AsyncErrorClassifier {

    /**
     * 按优先级排序后的业务与扩展分类规则。
     */
    private final List<AsyncErrorClassificationRule> rules;

    /**
     * 当所有扩展规则均不匹配时使用的默认技术分类器。
     */
    private final AsyncErrorClassifier fallbackClassifier;

    /**
     * 创建组合分类器。
     *
     * @param rules 业务和扩展分类规则
     * @param fallbackClassifier 默认技术分类器
     */
    public CompositeAsyncErrorClassifier(
            List<AsyncErrorClassificationRule> rules,
            AsyncErrorClassifier fallbackClassifier
    ) {
        this.rules = rules == null
                ? List.of()
                : rules.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(AsyncErrorClassificationRule::order))
                .toList();
        this.fallbackClassifier = Objects.requireNonNull(
                fallbackClassifier,
                "fallbackClassifier must not be null"
        );
    }

    /**
     * 按规则链分类异常。
     *
     * @param task 异步任务
     * @param throwable 原始异常
     * @param stage 异常发生阶段
     * @return 结构化错误
     */
    @Override
    public AsyncError classify(
            AsyncTask<?> task,
            Throwable throwable,
            AsyncErrorStage stage
    ) {
        AsyncErrorClassificationContext context = new AsyncErrorClassificationContext(
                task,
                throwable,
                CompletableFutureExceptionUtils.rootCause(throwable),
                stage
        );

        for (AsyncErrorClassificationRule rule : rules) {
            try {
                if (!rule.supports(context)) {
                    continue;
                }

                AsyncError error = rule.classify(context);
                if (error != null) {
                    return error;
                }
            } catch (Throwable ignored) {
                /*
                 * 错误分类属于旁路诊断能力，单条业务规则异常不能覆盖原始任务异常。
                 * 当前无日志依赖，直接继续尝试后续规则，最终由默认分类器兜底。
                 */
            }
        }

        return fallbackClassifier.classify(task, throwable, stage);
    }

    /**
     * 获取已装配的规则数量，便于诊断和测试。
     *
     * @return 规则数量
     */
    public int ruleCount() {
        return rules.size();
    }
}
