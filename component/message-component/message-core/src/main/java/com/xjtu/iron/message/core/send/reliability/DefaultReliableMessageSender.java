package com.xjtu.iron.message.core.send.reliability;

import com.xjtu.iron.message.core.send.MessageSendExecutor;
import com.xjtu.iron.message.core.send.MessageSendReliabilityOptions;
import com.xjtu.iron.message.core.send.PreparedMessageSend;

import com.xjtu.iron.message.api.publish.SendFailureType;
import com.xjtu.iron.message.api.publish.SendReliabilityInfo;
import com.xjtu.iron.message.api.publish.SendResult;
import com.xjtu.iron.message.api.publish.SendStage;
import com.xjtu.iron.message.api.publish.SendStatus;
import com.xjtu.iron.message.spi.ProviderSendResult;
import com.xjtu.iron.retry.api.execution.RetryExecution;
import com.xjtu.iron.retry.api.execution.RetryExecutor;
import com.xjtu.iron.retry.api.execution.RetryResult;
import com.xjtu.iron.retry.api.policy.RetryFailureCategory;
import com.xjtu.iron.retry.api.policy.RetryPolicy;
import com.xjtu.iron.retry.api.policy.RetryPolicyRegistry;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于 retry-component 的可靠发送执行器。
 *
 * <p>
 * 这个类负责包装 Provider.send，并把 ProviderSendResult 交给 retry-component 判定。
 * retry-component 仍然只处理通用重试流程，不理解 Kafka/Pulsar/RocketMQ 的业务含义。
 * </p>
 */
public final class DefaultReliableMessageSender implements MessageSendExecutor {

    /** retry-component 执行入口。 */
    private final RetryExecutor retryExecutor;

    /** retry 策略注册表。 */
    private final RetryPolicyRegistry retryPolicyRegistry;

    /** 可靠发送参数。 */
    private final MessageSendReliabilityOptions reliabilityOptions;

    /** 组件统一时钟。 */
    private final Clock clock;

    /** 异步发送包装执行器。 */
    private final Executor asyncExecutor;

    public DefaultReliableMessageSender(
            RetryExecutor retryExecutor,
            RetryPolicyRegistry retryPolicyRegistry,
            MessageSendReliabilityOptions reliabilityOptions,
            Clock clock,
            Executor asyncExecutor) {
        this.retryExecutor = Objects.requireNonNull(retryExecutor, "retryExecutor must not be null");
        this.retryPolicyRegistry = Objects.requireNonNull(
                retryPolicyRegistry,
                "retryPolicyRegistry must not be null");
        this.reliabilityOptions = Objects.requireNonNull(
                reliabilityOptions,
                "reliabilityOptions must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor must not be null");
    }

    @Override
    public SendResult send(PreparedMessageSend prepared) {
        Objects.requireNonNull(prepared, "prepared must not be null");
        String retryId = createRetryId(prepared);
        RetryPolicy retryPolicy = createMessageSendPolicy();
        RetryExecution<ProviderSendResult> execution = RetryExecution
                .builder("message-send", context -> doProviderSendAttempt(prepared), retryPolicy)
                .retryId(retryId)
                .attributes(retryAttributes(prepared))
                .build();
        RetryResult<ProviderSendResult> retryResult = retryExecutor.execute(execution);
        return mapRetryResult(prepared, retryResult);
    }

    @Override
    public CompletionStage<SendResult> sendAsync(PreparedMessageSend prepared) {
        return CompletableFuture.supplyAsync(() -> send(prepared), asyncExecutor);
    }

    /**
     * 执行一次物理 Provider 发送尝试。
     */
    private ProviderSendResult doProviderSendAttempt(PreparedMessageSend prepared) {
        try {
            CompletionStage<ProviderSendResult> providerStage = prepared.provider().send(prepared.request());
            if (providerStage == null) {
                return ProviderSendResult.failed(
                        SendStatus.FAILED,
                        SendFailureType.CLIENT_ERROR,
                        "provider returned null completion stage");
            }
            ProviderSendResult providerResult = providerStage.toCompletableFuture().get(
                    prepared.confirmTimeout().toMillis(),
                    TimeUnit.MILLISECONDS);
            if (providerResult == null) {
                return ProviderSendResult.failed(
                        SendStatus.FAILED,
                        SendFailureType.CLIENT_ERROR,
                        "provider returned null send result");
            }
            return providerResult;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ProviderSendResult.failed(
                    SendStatus.UNKNOWN,
                    SendFailureType.INTERRUPTED,
                    "thread interrupted while waiting for send confirmation");
        } catch (TimeoutException exception) {
            return ProviderSendResult.failed(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    "send confirmation timeout after " + prepared.confirmTimeout());
        } catch (ExecutionException exception) {
            return classifyProviderThrowable(unwrap(exception));
        } catch (RuntimeException exception) {
            return classifyProviderThrowable(exception);
        }
    }

    /**
     * 将 Provider 抛出的异常转换为标准 ProviderSendResult。
     */
    private ProviderSendResult classifyProviderThrowable(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return ProviderSendResult.failed(
                    SendStatus.UNKNOWN,
                    SendFailureType.TIMEOUT,
                    throwable.getMessage());
        }
        return ProviderSendResult.failed(
                SendStatus.UNKNOWN,
                SendFailureType.UNKNOWN_OUTCOME,
                throwable == null ? "unknown provider failure" : throwable.getMessage());
    }

    /**
     * 基于配置中的 message-send 策略创建消息发送专用策略。
     */
    private RetryPolicy createMessageSendPolicy() {
        RetryPolicy basePolicy = retryPolicyRegistry.getRequired(reliabilityOptions.retryPolicyName());
        return RetryPolicy.builder(basePolicy.getPolicyName())
                .maxAttempts(basePolicy.getMaxAttempts())
                .maxDuration(basePolicy.getMaxDuration())
                .operationSafety(basePolicy.getOperationSafety())
                .safetyMode(basePolicy.getSafetyMode())
                .traverseCauses(basePolicy.isTraverseCauses())
                .maxCauseDepth(basePolicy.getMaxCauseDepth())
                .backoffStrategy(basePolicy.getBackoffStrategy())
                .classifier(new MessageSendRetryClassifier(reliabilityOptions.retryWhenUnknown()))
                .build();
    }

    private SendResult mapRetryResult(
            PreparedMessageSend prepared,
            RetryResult<ProviderSendResult> retryResult) {
        SendReliabilityInfo reliabilityInfo = reliabilityInfo(retryResult);
        return switch (retryResult.getStatus()) {
            case SUCCESS -> mapProviderResult(prepared, retryResult.getValue(), reliabilityInfo);
            case EXHAUSTED -> retryExhaustedResult(prepared, retryResult, reliabilityInfo);
            case NOT_RETRYABLE -> notRetryableResult(prepared, retryResult, reliabilityInfo);
            case TIMED_OUT -> failureResult(prepared, SendStatus.UNKNOWN, SendStage.RETRY,
                    SendFailureType.TIMEOUT, "retry max duration timed out", reliabilityInfo);
            case INTERRUPTED -> failureResult(prepared, SendStatus.UNKNOWN, SendStage.RETRY,
                    SendFailureType.INTERRUPTED, "retry execution interrupted", reliabilityInfo);
            case CANCELLED, ABORTED -> failureResult(prepared, SendStatus.FAILED, SendStage.RETRY,
                    SendFailureType.RETRY_EXECUTION_ERROR,
                    "retry execution " + retryResult.getStatus(), reliabilityInfo);
            case EXECUTION_FAILED -> failureResult(prepared, SendStatus.FAILED, SendStage.RETRY,
                    SendFailureType.RETRY_EXECUTION_ERROR,
                    retryResult.getFailure() == null
                            ? "retry execution failed"
                            : retryResult.getFailure().getMessage(),
                    reliabilityInfo);
        };
    }

    private SendReliabilityInfo reliabilityInfo(RetryResult<ProviderSendResult> retryResult) {
        if (!reliabilityOptions.includeReliabilityInfo()) {
            return SendReliabilityInfo.disabled();
        }
        RetryFailureCategory failureCategory = retryResult.getFailureCategory();
        return SendReliabilityInfo.enabled(
                retryResult.getRetryId(),
                retryResult.getPolicyName(),
                retryResult.getStatus().name(),
                retryResult.getAttempts(),
                retryResult.getFailureCode(),
                failureCategory == null ? null : failureCategory.name());
    }

    private SendResult retryExhaustedResult(
            PreparedMessageSend prepared,
            RetryResult<ProviderSendResult> retryResult,
            SendReliabilityInfo reliabilityInfo) {
        ProviderSendResult lastProviderResult = lastProviderResult(retryResult);
        if (lastProviderResult != null && lastProviderResult.status() == SendStatus.UNKNOWN) {
            return failureResult(prepared, SendStatus.UNKNOWN, SendStage.RETRY,
                    SendFailureType.RETRY_EXHAUSTED,
                    "retry exhausted but last send outcome is unknown",
                    reliabilityInfo);
        }
        return failureResult(prepared, SendStatus.FAILED, SendStage.RETRY,
                SendFailureType.RETRY_EXHAUSTED,
                "message send retry exhausted",
                reliabilityInfo);
    }

    private SendResult notRetryableResult(
            PreparedMessageSend prepared,
            RetryResult<ProviderSendResult> retryResult,
            SendReliabilityInfo reliabilityInfo) {
        ProviderSendResult lastProviderResult = lastProviderResult(retryResult);
        if (lastProviderResult != null) {
            return mapProviderResult(prepared, lastProviderResult, reliabilityInfo);
        }
        return failureResult(prepared, SendStatus.FAILED, SendStage.RETRY,
                SendFailureType.UNKNOWN_ERROR,
                "message send stopped by retry classifier",
                reliabilityInfo);
    }

    private SendResult mapProviderResult(
            PreparedMessageSend prepared,
            ProviderSendResult providerResult,
            SendReliabilityInfo reliabilityInfo) {
        SendStage stage = switch (providerResult.status()) {
            case CONFIRMED -> SendStage.COMPLETE;
            case UNKNOWN -> SendStage.CONFIRM;
            case REJECTED, FAILED -> SendStage.SEND;
        };
        return new SendResult(
                prepared.message().messageId(),
                prepared.destination(),
                prepared.providerDestination().providerName(),
                prepared.providerDestination().physicalName(),
                providerResult.status(),
                stage,
                providerResult.failureType(),
                providerResult.providerMessageId(),
                providerResult.description(),
                prepared.startedAt(),
                clock.instant(),
                providerResult.metadata(),
                reliabilityInfo);
    }

    private SendResult failureResult(
            PreparedMessageSend prepared,
            SendStatus status,
            SendStage stage,
            SendFailureType failureType,
            String description,
            SendReliabilityInfo reliabilityInfo) {
        return new SendResult(
                prepared.message().messageId(),
                prepared.destination(),
                prepared.providerDestination().providerName(),
                prepared.providerDestination().physicalName(),
                status,
                stage,
                failureType,
                null,
                description,
                prepared.startedAt(),
                clock.instant(),
                Map.of(),
                reliabilityInfo);
    }

    private static ProviderSendResult lastProviderResult(RetryResult<ProviderSendResult> retryResult) {
        if (retryResult.getLastAttempt() == null) {
            return null;
        }
        return retryResult.getLastAttempt().getResult();
    }

    private static Map<String, Object> retryAttributes(PreparedMessageSend prepared) {
        String messageKey = prepared.message().messageKey() == null
                ? ""
                : prepared.message().messageKey();
        return Map.of(
                "messageId", prepared.message().messageId(),
                "provider", prepared.providerDestination().providerName(),
                "destination", prepared.destination().qualifiedName(),
                "physicalDestination", prepared.providerDestination().physicalName(),
                "messageKey", messageKey);
    }

    private static String createRetryId(PreparedMessageSend prepared) {
        return "message-send-"
                + prepared.providerDestination().providerName()
                + "-"
                + prepared.message().messageId()
                + "-"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
