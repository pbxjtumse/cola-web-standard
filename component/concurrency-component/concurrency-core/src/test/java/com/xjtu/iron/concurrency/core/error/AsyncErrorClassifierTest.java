package com.xjtu.iron.concurrency.core.error;

import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorCategory;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorReason;
import com.xjtu.iron.concurrency.api.enums.error.AsyncErrorStage;
import com.xjtu.iron.concurrency.api.error.AsyncError;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassification;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationContext;
import com.xjtu.iron.concurrency.api.error.AsyncErrorClassificationRule;
import com.xjtu.iron.concurrency.api.task.TaskMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncErrorClassifierTest {

    private final TaskMetadata metadata = new TaskMetadata(
            "task-1",
            "pool",
            "task",
            "biz",
            "desc",
            null
    );

    @Test
    void compositeUsesFirstSupportedRuleByOrder() {
        AsyncError expected = error(AsyncErrorCategory.APPLICATION, AsyncErrorReason.TASK_THROWN);
        AsyncErrorClassificationRule later = new FixedRule(10, true, error(AsyncErrorCategory.UNKNOWN, AsyncErrorReason.UNKNOWN));
        AsyncErrorClassificationRule earlier = new FixedRule(-1, true, expected);

        CompositeAsyncErrorClassifier classifier = new CompositeAsyncErrorClassifier(
                List.of(later, earlier),
                new DefaultAsyncErrorClassifier()
        );

        AsyncError actual = classifier.classify(metadata, new RuntimeException("x"), AsyncErrorStage.RUN);

        assertEquals(2, classifier.ruleCount());
        assertEquals(expected.getClassification().getCategory(), actual.getClassification().getCategory());
    }

    @Test
    void compositeSkipsUnsupportedNullAndFailingRules() {
        AsyncError expected = error(AsyncErrorCategory.APPLICATION, AsyncErrorReason.TASK_THROWN);
        AsyncErrorClassificationRule unsupported = new FixedRule(0, false, expected);
        AsyncErrorClassificationRule failing = new FailingRule();
        AsyncErrorClassificationRule supported = new FixedRule(2, true, expected);

        CompositeAsyncErrorClassifier classifier = new CompositeAsyncErrorClassifier(
                List.of(null, unsupported, failing, supported),
                new DefaultAsyncErrorClassifier()
        );

        AsyncError actual = classifier.classify(metadata, new RuntimeException("x"), AsyncErrorStage.RUN);

        assertEquals(3, classifier.ruleCount());
        assertEquals(AsyncErrorCategory.APPLICATION, actual.getClassification().getCategory());
    }

    @Test
    void defaultClassifierClassifiesRejectedTimeoutAndUnknown() {
        DefaultAsyncErrorClassifier classifier = new DefaultAsyncErrorClassifier();

        assertEquals(
                AsyncErrorReason.REJECTED,
                classifier.classify(metadata, new RejectedExecutionException("reject"), AsyncErrorStage.SUBMIT)
                        .getClassification()
                        .getReason()
        );
        assertEquals(
                AsyncErrorReason.QUEUE_TIMEOUT,
                classifier.classify(metadata, new TimeoutException("queue"), AsyncErrorStage.QUEUE)
                        .getClassification()
                        .getReason()
        );
        assertEquals(
                AsyncErrorReason.UNKNOWN,
                classifier.classify(metadata, new Exception("unknown"), AsyncErrorStage.RUN)
                        .getClassification()
                        .getReason()
        );
    }

    @Test
    void classificationContextKeepsTaskMetadataAndUnwrapsCompletionWrapperOnly() {
        RuntimeException root = new RuntimeException("root");
        java.util.concurrent.CompletionException wrapper = new java.util.concurrent.CompletionException(root);

        AsyncErrorClassificationContext context = AsyncErrorClassificationContext.of(
                metadata,
                wrapper,
                AsyncErrorStage.RUN
        );

        assertSame(metadata, context.getTask());
        assertSame(wrapper, context.getThrowable());
        assertSame(root, context.getUnwrapped());
        assertSame(root, context.getRootCause());
    }

    private AsyncError error(AsyncErrorCategory category, AsyncErrorReason reason) {
        return AsyncError.builder()
                .classification(AsyncErrorClassification.of(category, reason, AsyncErrorStage.RUN))
                .build();
    }

    private record FixedRule(int order, boolean supports, AsyncError error)
            implements AsyncErrorClassificationRule {
        @Override
        public boolean supports(AsyncErrorClassificationContext context) {
            return supports;
        }

        @Override
        public AsyncError classify(AsyncErrorClassificationContext context) {
            return error;
        }
    }

    private static final class FailingRule implements AsyncErrorClassificationRule {
        @Override
        public boolean supports(AsyncErrorClassificationContext context) {
            return true;
        }

        @Override
        public AsyncError classify(AsyncErrorClassificationContext context) {
            throw new IllegalStateException("bad rule");
        }

        @Override
        public int order() {
            return 1;
        }
    }
}
