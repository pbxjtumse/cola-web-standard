package com.xjtu.iron.concurrency.api.error;


import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationContext;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationRule;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;


import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 组合异步错误分类器。
 *
 * <p>
 * 按优先级遍历分类规则，第一个匹配的规则负责生成 AsyncError。
 * 如果没有任何业务规则匹配，则交给默认技术分类器。
 * </p>
 */
public class CompositeAsyncErrorClassifier implements AsyncErrorClassifier {

    /**
     * 业务和扩展分类规则。
     */
    private final List<AsyncErrorClassificationRule> rules;

    /**
     * 默认技术错误分类器。
     */
    private final AsyncErrorClassifier fallbackClassifier;

    public CompositeAsyncErrorClassifier(
            List<AsyncErrorClassificationRule> rules,
            AsyncErrorClassifier fallbackClassifier
    ) {
        this.rules = rules == null
                ? List.of()
                : rules.stream()
                  .filter(Objects::nonNull)
                  .sorted(Comparator.comparingInt(
                          AsyncErrorClassificationRule::order
                  ))
                  .toList();

        this.fallbackClassifier = Objects.requireNonNull(
                fallbackClassifier,
                "fallbackClassifier must not be null"
        );
    }

    @Override
    public AsyncError classify(
            AsyncTask<?> task,
            Throwable throwable,
            AsyncErrorStage stage
    ) {
        Throwable rootCause =
                CompletableFutureExceptionUtils.rootCause(throwable);

        AsyncErrorClassificationContext context =
                new AsyncErrorClassificationContext(
                        task,
                        throwable,
                        rootCause,
                        stage
                );

        for (AsyncErrorClassificationRule rule : rules) {
            if (rule.supports(context)) {
                return rule.classify(context);
            }
        }

        return fallbackClassifier.classify(task, throwable, stage);
    }
}
