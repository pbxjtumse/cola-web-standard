package com.xjtu.iron.retry.api;

import com.xjtu.iron.retry.api.support.BackoffStrategies;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 一次重试执行使用的不可变策略快照。
 */
public final class RetryPolicy {

    /**
     * 策略名称。
     */
    private final String policyName;

    /**
     * 最大尝试次数，包含第一次正常执行。
     */
    private final int maxAttempts;

    /**
     * 整个逻辑执行允许使用的最大持续时间。
     */
    private final Duration maxDuration;

    /**
     * 当前操作的重复执行安全级别。
     */
    private final OperationSafety operationSafety;

    /**
     * 单次尝试完成后的分类器。
     */
    private final RetryClassifier retryClassifier;

    /**
     * 下一次尝试前的退避策略。
     */
    private final BackoffStrategy backoffStrategy;

    private RetryPolicy(Builder builder) {
        this.policyName = requireText(builder.policyName, "policyName");
        if (builder.maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
        }
        this.maxAttempts = builder.maxAttempts;
        this.maxDuration = requirePositive(builder.maxDuration, "maxDuration");
        this.operationSafety = Objects.requireNonNull(builder.operationSafety, "operationSafety must not be null");
        this.backoffStrategy = Objects.requireNonNull(builder.backoffStrategy, "backoffStrategy must not be null");
        this.retryClassifier = builder.customClassifier == null
                ? new ConfiguredRetryClassifier(
                        builder.retryableExceptions,
                        builder.stoppedExceptions,
                        builder.retryResultPredicate)
                : builder.customClassifier;
    }

    public static Builder builder(String policyName) {
        return new Builder(policyName);
    }

    public String getPolicyName() {
        return policyName;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getMaxDuration() {
        return maxDuration;
    }

    public OperationSafety getOperationSafety() {
        return operationSafety;
    }

    public RetryClassifier getRetryClassifier() {
        return retryClassifier;
    }

    public BackoffStrategy getBackoffStrategy() {
        return backoffStrategy;
    }

    /**
     * 重试策略构建器。
     */
    public static final class Builder {

        private final String policyName;
        private int maxAttempts = 3;
        private Duration maxDuration = Duration.ofSeconds(5);
        private OperationSafety operationSafety = OperationSafety.UNSPECIFIED;
        private BackoffStrategy backoffStrategy = BackoffStrategies.none();
        private final List<Class<? extends Throwable>> retryableExceptions = new ArrayList<>();
        private final List<Class<? extends Throwable>> stoppedExceptions = new ArrayList<>();
        private Predicate<Object> retryResultPredicate;
        private RetryClassifier customClassifier;

        private Builder(String policyName) {
            this.policyName = policyName;
        }

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }

        public Builder operationSafety(OperationSafety operationSafety) {
            this.operationSafety = operationSafety;
            return this;
        }

        @SafeVarargs
        public final Builder retryOn(Class<? extends Throwable>... exceptionTypes) {
            addExceptionTypes(retryableExceptions, exceptionTypes, "retryOn");
            return this;
        }

        @SafeVarargs
        public final Builder stopOn(Class<? extends Throwable>... exceptionTypes) {
            addExceptionTypes(stoppedExceptions, exceptionTypes, "stopOn");
            return this;
        }

        /**
         * 与 stopOn 语义一致，用于兼容常见的 abortOn 表达方式。
         */
        @SafeVarargs
        public final Builder abortOn(Class<? extends Throwable>... exceptionTypes) {
            return stopOn(exceptionTypes);
        }

        public Builder retryIfResult(Predicate<Object> retryResultPredicate) {
            this.retryResultPredicate = Objects.requireNonNull(
                    retryResultPredicate,
                    "retryResultPredicate must not be null"
            );
            return this;
        }

        /**
         * 使用完整自定义分类器。
         *
         * <p>配置该分类器后，retryOn、stopOn 和 retryIfResult 生成的默认分类规则不再生效。</p>
         */
        public Builder classifier(RetryClassifier retryClassifier) {
            this.customClassifier = Objects.requireNonNull(retryClassifier, "retryClassifier must not be null");
            return this;
        }

        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = Objects.requireNonNull(backoffStrategy, "backoffStrategy must not be null");
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }

        private static void addExceptionTypes(
                List<Class<? extends Throwable>> target,
                Class<? extends Throwable>[] exceptionTypes,
                String methodName) {
            Objects.requireNonNull(exceptionTypes, methodName + " exceptionTypes must not be null");
            for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                target.add(Objects.requireNonNull(
                        exceptionType,
                        methodName + " exceptionType must not be null"
                ));
            }
        }
    }

    /**
     * 由构建器配置生成的保守分类器。
     */
    private static final class ConfiguredRetryClassifier implements RetryClassifier {

        private final List<Class<? extends Throwable>> retryableExceptions;
        private final List<Class<? extends Throwable>> stoppedExceptions;
        private final Predicate<Object> retryResultPredicate;

        private ConfiguredRetryClassifier(
                List<Class<? extends Throwable>> retryableExceptions,
                List<Class<? extends Throwable>> stoppedExceptions,
                Predicate<Object> retryResultPredicate) {
            this.retryableExceptions = Collections.unmodifiableList(new ArrayList<>(retryableExceptions));
            this.stoppedExceptions = Collections.unmodifiableList(new ArrayList<>(stoppedExceptions));
            this.retryResultPredicate = retryResultPredicate;
        }

        @Override
        public RetryDecision classify(RetryContext context, Object result, Throwable failure) {
            if (failure == null) {
                if (retryResultPredicate != null && retryResultPredicate.test(result)) {
                    return RetryDecision.RETRY;
                }
                return RetryDecision.SUCCESS;
            }

            if (failure instanceof InterruptedException) {
                return RetryDecision.ABORT;
            }

            if (matches(stoppedExceptions, failure)) {
                return RetryDecision.STOP;
            }

            if (matches(retryableExceptions, failure)) {
                return RetryDecision.RETRY;
            }

            return RetryDecision.STOP;
        }

        private static boolean matches(List<Class<? extends Throwable>> configuredTypes, Throwable failure) {
            for (Class<? extends Throwable> configuredType : configuredTypes) {
                if (configuredType.isInstance(failure)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
