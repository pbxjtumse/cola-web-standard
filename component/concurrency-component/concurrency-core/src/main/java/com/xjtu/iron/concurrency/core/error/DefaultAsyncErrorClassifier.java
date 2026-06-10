package com.xjtu.iron.concurrency.core.error;


import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorCategory;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorReason;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.error.AsyncRecoveryAction;
import com.xjtu.iron.concurrency.api.error.*;

import com.xjtu.iron.concurrency.api.exception.ConcurrencyException;
import com.xjtu.iron.concurrency.api.exception.ConcurrencyRejectedException;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;


import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

/**
 * 默认异步错误分类器。
 *
 * <p>
 * 默认实现只识别并行组件和 JDK 常见异常。
 * 业务异常、领域异常、应用异常需要业务系统自定义 AsyncErrorClassifier 覆盖。
 * </p>
 */
public class DefaultAsyncErrorClassifier implements AsyncErrorClassifier {

    @Override
    public AsyncError classify(AsyncTask<?> task, Throwable throwable, AsyncErrorStage stage) {
        Throwable root = CompletableFutureExceptionUtils.rootCause(throwable);

        if (root == null) {
            return AsyncError.none();
        }

        if (root instanceof ConcurrencyRejectedException) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.RESOURCE,
                            AsyncErrorReason.REJECTED,
                            stage == null ? AsyncErrorStage.SUBMIT : stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.DELAY_RETRY,
                            true,
                            false,
                            true
                    ))
                    .build();
        }

        if (root instanceof TimeoutException) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.DEPENDENCY,
                            AsyncErrorReason.TIMEOUT,
                            stage == null ? AsyncErrorStage.WAIT_RESULT : stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.FAST_RETRY,
                            true,
                            false,
                            false
                    ))
                    .build();
        }

        if (root instanceof CancellationException) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.COMPONENT,
                            AsyncErrorReason.CANCELLED,
                            stage == null ? AsyncErrorStage.CANCEL : stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.none())
                    .build();
        }

        if (root instanceof ConcurrencyException) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.COMPONENT,
                            AsyncErrorReason.TASK_THROWN,
                            stage == null ? AsyncErrorStage.RUN : stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.ALERT,
                            false,
                            false,
                            true
                    ))
                    .build();
        }

        if (root instanceof NullPointerException
                || root instanceof ClassCastException
                || root instanceof IllegalStateException
                || root instanceof IllegalArgumentException) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.SYSTEM,
                            AsyncErrorReason.TASK_THROWN,
                            stage == null ? AsyncErrorStage.RUN : stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.ALERT,
                            false,
                            false,
                            true
                    ))
                    .build();
        }

        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(
                        AsyncErrorCategory.UNKNOWN,
                        AsyncErrorReason.UNKNOWN,
                        stage == null ? AsyncErrorStage.NONE : stage
                ))
                .application(ApplicationErrorInfo.none())
                .exception(ExceptionInfo.from(throwable))
                .recovery(RecoveryHint.of(
                        AsyncRecoveryAction.ALERT,
                        false,
                        false,
                        true
                ))
                .build();
    }
}
