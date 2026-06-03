package com.xjtu.iron.concurrency.api.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量异步任务执行结果。
 *
 * @param <T> 任务返回值类型
 */
public class AsyncBatchResult<T> {

    /**
     * 每个任务的执行结果。
     */
    private final List<AsyncTaskOutcome<T>> outcomes;

    public AsyncBatchResult(List<AsyncTaskOutcome<T>> outcomes) {
        this.outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }

    public List<AsyncTaskOutcome<T>> getOutcomes() {
        return outcomes;
    }

    public boolean isAllSuccess() {
        return outcomes.stream().allMatch(AsyncTaskOutcome::isSuccess);
    }

    public boolean hasFailure() {
        return outcomes.stream().anyMatch(outcome -> !outcome.isSuccess());
    }

    public List<T> successValues() {
        List<T> values = new ArrayList<>();

        for (AsyncTaskOutcome<T> outcome : outcomes) {
            if (outcome.isSuccess()) {
                values.add(outcome.getValue());
            }
        }

        return Collections.unmodifiableList(values);
    }

    public List<AsyncTaskOutcome<T>> failures() {
        List<AsyncTaskOutcome<T>> failures = new ArrayList<>();

        for (AsyncTaskOutcome<T> outcome : outcomes) {
            if (!outcome.isSuccess()) {
                failures.add(outcome);
            }
        }

        return Collections.unmodifiableList(failures);
    }
}
