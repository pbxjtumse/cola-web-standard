package com.xjtu.iron.domain.exception;



import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorCategory;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorReason;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.enums.error.AsyncRecoveryAction;
import com.xjtu.iron.concurrency.api.error.ApplicationErrorInfo;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassification;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassifier;
import com.xjtu.iron.concurrency.api.error.CompletableFutureExceptionUtils;
import com.xjtu.iron.concurrency.api.error.ExceptionInfo;
import com.xjtu.iron.concurrency.api.error.RecoveryHint;
import com.xjtu.iron.concurrency.api.execution.task.AsyncTask;


import com.xjtu.iron.concurrency.core.error.DefaultAsyncErrorClassifier;
import org.springframework.stereotype.Component;

package com.xjtu.iron.domain.exception;
/**
 * 营销业务异步错误分类器。
 *
 * <p>
 * 这个类在业务工程中实现，用于把业务异常、领域异常、RPC 异常映射成并行组件可识别的 AsyncError。
 * </p>
 */
@Component
public class MarketingAsyncErrorClassifier implements AsyncErrorClassifier {

    /**
     * 默认分类器。
     *
     * <p>
     * 业务分类器只识别自己认识的异常；
     * 不认识的异常交给默认分类器处理。
     * </p>
     */
    private final DefaultAsyncErrorClassifier delegate = new DefaultAsyncErrorClassifier();

    @Override
    public AsyncError classify(AsyncTask<?> task, Throwable throwable, AsyncErrorStage stage) {
        Throwable root = CompletableFutureExceptionUtils.rootCause(throwable);

        if (root instanceof DomainException ex) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.APPLICATION,
                            AsyncErrorReason.TASK_THROWN,
                            stage
                    ))
                    .application(ApplicationErrorInfo.of(
                            ex.getErrorCode(),
                            ex.getSceneCode(),
                            ex.getMessage()
                    ))
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.COMPENSATE,
                            false,
                            true,
                            true
                    ))
                    .build();
        }

        if (root instanceof RpcException ex) {
            return AsyncError.builder()
                    .classification(AsyncErrorClassification.of(
                            AsyncErrorCategory.DEPENDENCY,
                            AsyncErrorReason.TASK_THROWN,
                            stage
                    ))
                    .application(ApplicationErrorInfo.none())
                    .exception(ExceptionInfo.from(throwable))
                    .recovery(RecoveryHint.of(
                            AsyncRecoveryAction.FAST_RETRY,
                            true,
                            false,
                            false
                    ))
                    .attribute("remoteService", ex.getRemoteService())
                    .build();
        }

        return delegate.classify(task, throwable, stage);
    }
}
