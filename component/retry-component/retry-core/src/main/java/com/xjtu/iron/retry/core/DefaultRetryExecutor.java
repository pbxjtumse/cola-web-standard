package com.xjtu.iron.retry.core;

import com.xjtu.iron.retry.api.OperationSafety;
import com.xjtu.iron.retry.api.RetryContext;
import com.xjtu.iron.retry.api.RetryDecision;
import com.xjtu.iron.retry.api.RetryEvent;
import com.xjtu.iron.retry.api.RetryEventType;
import com.xjtu.iron.retry.api.RetryExecutor;
import com.xjtu.iron.retry.api.RetryListener;
import com.xjtu.iron.retry.api.RetryOperation;
import com.xjtu.iron.retry.api.RetryPolicy;
import com.xjtu.iron.retry.api.RetryPolicyRegistry;
import com.xjtu.iron.retry.api.RetryResult;
import com.xjtu.iron.retry.api.RetryStatus;
import com.xjtu.iron.retry.core.time.RetrySleeper;
import com.xjtu.iron.retry.core.time.ThreadSleepRetrySleeper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 进程内同步重试默认实现。
 *
 * <p>该实现只捕获 Exception，不捕获 Error，避免吞掉虚拟机级严重错误。</p>
 */
public final class DefaultRetryExecutor implements RetryExecutor {

    private static final System.Logger LOGGER = System.getLogger(DefaultRetryExecutor.class.getName());

    private final RetryPolicyRegistry retryPolicyRegistry;
    private final List<RetryListener> retryListeners;
    private final RetrySleeper retrySleeper;

    public DefaultRetryExecutor() {
        this(new DefaultRetryPolicyRegistry(), Collections.emptyList(), new ThreadSleepRetrySleeper());
    }

    public DefaultRetryExecutor(RetryPolicyRegistry retryPolicyRegistry) {
        this(retryPolicyRegistry, Collections.emptyList(), new ThreadSleepRetrySleeper());
    }

    public DefaultRetryExecutor(
            RetryPolicyRegistry retryPolicyRegistry,
            Collection<RetryListener> retryListeners) {
        this(retryPolicyRegistry, retryListeners, new ThreadSleepRetrySleeper());
    }

    public DefaultRetryExecutor(
            RetryPolicyRegistry retryPolicyRegistry,
            Collection<RetryListener> retryListeners,
            RetrySleeper retrySleeper) {
        this.retryPolicyRegistry = Objects.requireNonNull(
                retryPolicyRegistry,
                "retryPolicyRegistry must not be null"
        );
        Collection<RetryListener> actualListeners = retryListeners == null
                ? Collections.emptyList()
                : retryListeners;
        this.retryListeners = Collections.unmodifiableList(new ArrayList<>(actualListeners));
        this.retrySleeper = Objects.requireNonNull(retrySleeper, "retrySleeper must not be null");
    }

    @Override
    public <T> RetryResult<T> execute(
            String operationName,
            RetryOperation<T> operation,
            String policyName) {
        RetryPolicy retryPolicy = retryPolicyRegistry.getRequired(policyName);
        return execute(operationName, operation, retryPolicy);
    }

    @Override
    public <T> RetryResult<T> execute(
            String operationName,
            RetryOperation<T> operation,
            RetryPolicy retryPolicy) {
        String actualOperationName = requireText(operationName, "operationName");
        RetryOperation<T> actualOperation = Objects.requireNonNull(operation, "operation must not be null");
        RetryPolicy actualPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");

        warnUnsafeOperation(actualOperationName, actualPolicy);

        String retryId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        long startNanos = System.nanoTime();
        int maxAttempts = actualPolicy.getMaxAttempts();
        T lastResult = null;
        Throwable lastFailure = null;

        publish(event(
                RetryEventType.EXECUTION_STARTED,
                retryId,
                actualOperationName,
                actualPolicy,
                0,
                durationSince(startNanos),
                Duration.ZERO,
                Duration.ZERO,
                null,
                null
        ));

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Duration beforeAttemptElapsed = durationSince(startNanos);

            if (Thread.currentThread().isInterrupted()) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.INTERRUPTED,
                        lastResult,
                        lastFailure,
                        attempt - 1,
                        beforeAttemptElapsed,
                        RetryEventType.INTERRUPTED,
                        Duration.ZERO
                );
            }

            if (beforeAttemptElapsed.compareTo(actualPolicy.getMaxDuration()) >= 0) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.TIMED_OUT,
                        lastResult,
                        lastFailure,
                        attempt - 1,
                        beforeAttemptElapsed,
                        RetryEventType.TIMED_OUT,
                        Duration.ZERO
                );
            }

            RetryContext operationContext = new RetryContext(
                    retryId,
                    actualOperationName,
                    actualPolicy.getPolicyName(),
                    attempt,
                    startTime,
                    beforeAttemptElapsed,
                    lastResult,
                    lastFailure,
                    Map.of()
            );

            publish(event(
                    RetryEventType.ATTEMPT_STARTED,
                    retryId,
                    actualOperationName,
                    actualPolicy,
                    attempt,
                    beforeAttemptElapsed,
                    Duration.ZERO,
                    Duration.ZERO,
                    null,
                    null
            ));

            long attemptStartNanos = System.nanoTime();
            T currentResult = null;
            Throwable currentFailure = null;

            try {
                currentResult = actualOperation.execute(operationContext);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                Duration attemptDuration = durationSince(attemptStartNanos);
                Duration elapsed = durationSince(startNanos);
                publish(event(
                        RetryEventType.ATTEMPT_COMPLETED,
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        attempt,
                        elapsed,
                        attemptDuration,
                        Duration.ZERO,
                        null,
                        interruptedException
                ));
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.INTERRUPTED,
                        null,
                        interruptedException,
                        attempt,
                        elapsed,
                        RetryEventType.INTERRUPTED,
                        attemptDuration
                );
            } catch (Exception exception) {
                currentFailure = exception;
            }

            Duration attemptDuration = durationSince(attemptStartNanos);
            Duration elapsed = durationSince(startNanos);
            RetryContext outcomeContext = operationContext.withOutcome(elapsed, currentResult, currentFailure);

            publish(event(
                    RetryEventType.ATTEMPT_COMPLETED,
                    retryId,
                    actualOperationName,
                    actualPolicy,
                    attempt,
                    elapsed,
                    attemptDuration,
                    Duration.ZERO,
                    null,
                    currentFailure
            ));

            RetryDecision retryDecision;
            try {
                retryDecision = actualPolicy.getRetryClassifier().classify(
                        outcomeContext,
                        currentResult,
                        currentFailure
                );
            } catch (RuntimeException classifierFailure) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXECUTION_FAILED,
                        currentResult,
                        classifierFailure,
                        attempt,
                        elapsed,
                        RetryEventType.ABORTED,
                        attemptDuration
                );
            }

            if (retryDecision == null) {
                IllegalStateException classifierFailure = new IllegalStateException(
                        "RetryClassifier returned null decision"
                );
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXECUTION_FAILED,
                        currentResult,
                        classifierFailure,
                        attempt,
                        elapsed,
                        RetryEventType.ABORTED,
                        attemptDuration
                );
            }

            if (retryDecision == RetryDecision.SUCCESS) {
                if (currentFailure != null) {
                    IllegalStateException classifierFailure = new IllegalStateException(
                            "RetryClassifier cannot mark an exceptional attempt as SUCCESS",
                            currentFailure
                    );
                    return terminal(
                            retryId,
                            actualOperationName,
                            actualPolicy,
                            RetryStatus.EXECUTION_FAILED,
                            currentResult,
                            classifierFailure,
                            attempt,
                            elapsed,
                            RetryEventType.ABORTED,
                            attemptDuration
                    );
                }
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.SUCCESS,
                        currentResult,
                        null,
                        attempt,
                        elapsed,
                        RetryEventType.SUCCEEDED,
                        attemptDuration
                );
            }

            if (retryDecision == RetryDecision.STOP) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.NOT_RETRYABLE,
                        currentResult,
                        currentFailure,
                        attempt,
                        elapsed,
                        RetryEventType.NOT_RETRYABLE,
                        attemptDuration
                );
            }

            if (retryDecision == RetryDecision.ABORT) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXECUTION_FAILED,
                        currentResult,
                        currentFailure,
                        attempt,
                        elapsed,
                        RetryEventType.ABORTED,
                        attemptDuration
                );
            }

            if (attempt >= maxAttempts) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXHAUSTED,
                        currentResult,
                        currentFailure,
                        attempt,
                        elapsed,
                        RetryEventType.EXHAUSTED,
                        attemptDuration
                );
            }

            if (elapsed.compareTo(actualPolicy.getMaxDuration()) >= 0) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.TIMED_OUT,
                        currentResult,
                        currentFailure,
                        attempt,
                        elapsed,
                        RetryEventType.TIMED_OUT,
                        attemptDuration
                );
            }

            Duration nextDelay;
            try {
                nextDelay = Objects.requireNonNull(
                        actualPolicy.getBackoffStrategy().nextDelay(outcomeContext),
                        "BackoffStrategy returned null delay"
                );
                if (nextDelay.isNegative()) {
                    throw new IllegalStateException("BackoffStrategy returned a negative delay");
                }
            } catch (RuntimeException backoffFailure) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXECUTION_FAILED,
                        currentResult,
                        backoffFailure,
                        attempt,
                        elapsed,
                        RetryEventType.ABORTED,
                        attemptDuration
                );
            }

            Duration remainingDuration = actualPolicy.getMaxDuration().minus(elapsed);
            if (nextDelay.compareTo(remainingDuration) > 0) {
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.TIMED_OUT,
                        currentResult,
                        currentFailure,
                        attempt,
                        elapsed,
                        RetryEventType.TIMED_OUT,
                        attemptDuration
                );
            }

            publish(event(
                    RetryEventType.RETRY_SCHEDULED,
                    retryId,
                    actualOperationName,
                    actualPolicy,
                    attempt,
                    elapsed,
                    attemptDuration,
                    nextDelay,
                    null,
                    currentFailure
            ));

            try {
                retrySleeper.sleep(nextDelay);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                Duration interruptedElapsed = durationSince(startNanos);
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.INTERRUPTED,
                        currentResult,
                        interruptedException,
                        attempt,
                        interruptedElapsed,
                        RetryEventType.INTERRUPTED,
                        attemptDuration
                );
            } catch (RuntimeException sleeperFailure) {
                Duration failedElapsed = durationSince(startNanos);
                return terminal(
                        retryId,
                        actualOperationName,
                        actualPolicy,
                        RetryStatus.EXECUTION_FAILED,
                        currentResult,
                        sleeperFailure,
                        attempt,
                        failedElapsed,
                        RetryEventType.ABORTED,
                        attemptDuration
                );
            }

            lastResult = currentResult;
            lastFailure = currentFailure;
        }

        throw new IllegalStateException("Retry execution reached an unreachable branch");
    }

    private <T> RetryResult<T> terminal(
            String retryId,
            String operationName,
            RetryPolicy retryPolicy,
            RetryStatus status,
            T value,
            Throwable failure,
            int attempts,
            Duration elapsedTime,
            RetryEventType eventType,
            Duration attemptDuration) {
        RetryResult<T> retryResult = RetryResult.of(
                retryId,
                operationName,
                retryPolicy.getPolicyName(),
                status,
                value,
                failure,
                attempts,
                elapsedTime
        );
        publish(event(
                eventType,
                retryId,
                operationName,
                retryPolicy,
                attempts,
                elapsedTime,
                attemptDuration,
                Duration.ZERO,
                status,
                failure
        ));
        return retryResult;
    }

    private RetryEvent event(
            RetryEventType eventType,
            String retryId,
            String operationName,
            RetryPolicy retryPolicy,
            int attempt,
            Duration elapsedTime,
            Duration attemptDuration,
            Duration nextDelay,
            RetryStatus finalStatus,
            Throwable failure) {
        return new RetryEvent(
                eventType,
                retryId,
                operationName,
                retryPolicy.getPolicyName(),
                attempt,
                retryPolicy.getMaxAttempts(),
                Instant.now(),
                elapsedTime,
                attemptDuration,
                nextDelay,
                finalStatus,
                failure
        );
    }

    private void publish(RetryEvent event) {
        for (RetryListener retryListener : retryListeners) {
            try {
                retryListener.onEvent(event);
            } catch (RuntimeException listenerFailure) {
                LOGGER.log(
                        System.Logger.Level.WARNING,
                        "RetryListener failed and was isolated: " + retryListener.getClass().getName(),
                        listenerFailure
                );
            }
        }
    }

    private static Duration durationSince(long startNanos) {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startNanos));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void warnUnsafeOperation(String operationName, RetryPolicy retryPolicy) {
        if (retryPolicy.getMaxAttempts() > 1
                && retryPolicy.getOperationSafety() == OperationSafety.NON_IDEMPOTENT) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Non-idempotent operation is configured with multiple attempts: operation="
                            + operationName
                            + ", policy="
                            + retryPolicy.getPolicyName()
            );
        }
    }
}
