package com.xjtu.iron.message.core.send;

import com.xjtu.iron.message.api.SendFailureType;
import com.xjtu.iron.message.api.SendReliabilityInfo;
import com.xjtu.iron.message.api.SendResult;
import com.xjtu.iron.message.api.SendStage;
import com.xjtu.iron.message.api.SendStatus;
import com.xjtu.iron.message.spi.ProviderSendResult;

import java.time.Clock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 一期直发执行器。
 *
 * <p>
 * 当发送可靠性关闭时使用。
 * 它不做 retry，只调用一次 Provider.send。
 * </p>
 */
public final class DirectMessageSender implements MessageSendExecutor {

    /** 组件统一时钟。 */
    private final Clock clock;

    public DirectMessageSender(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SendResult send(PreparedMessageSend prepared) {
        Objects.requireNonNull(prepared, "prepared must not be null");
        try {
            CompletionStage<ProviderSendResult> providerStage = prepared.provider().send(prepared.request());
            if (providerStage == null) {
                return failureResult(prepared, SendStatus.FAILED, SendStage.SEND,
                        SendFailureType.CLIENT_ERROR, "provider returned null completion stage");
            }
            ProviderSendResult providerResult = providerStage.toCompletableFuture().get(
                    prepared.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (providerResult == null) {
                return failureResult(prepared, SendStatus.FAILED, SendStage.CONFIRM,
                        SendFailureType.CLIENT_ERROR, "provider returned null send result");
            }
            return mapProviderResult(prepared, providerResult, SendReliabilityInfo.disabled());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failureResult(prepared, SendStatus.UNKNOWN, SendStage.CONFIRM,
                    SendFailureType.INTERRUPTED, "thread interrupted while waiting for send confirmation");
        } catch (TimeoutException exception) {
            return failureResult(prepared, SendStatus.UNKNOWN, SendStage.CONFIRM,
                    SendFailureType.TIMEOUT, "send confirmation timeout after " + prepared.confirmTimeout());
        } catch (ExecutionException exception) {
            return providerThrowableResult(prepared, unwrap(exception));
        } catch (RuntimeException exception) {
            return providerThrowableResult(prepared, exception);
        }
    }

    @Override
    public CompletionStage<SendResult> sendAsync(PreparedMessageSend prepared) {
        CompletableFuture<SendResult> resultFuture = new CompletableFuture<>();
        try {
            CompletionStage<ProviderSendResult> providerStage = prepared.provider().send(prepared.request());
            if (providerStage == null) {
                resultFuture.complete(failureResult(prepared, SendStatus.FAILED, SendStage.SEND,
                        SendFailureType.CLIENT_ERROR, "provider returned null completion stage"));
                return resultFuture;
            }
            providerStage.toCompletableFuture()
                    .orTimeout(prepared.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .whenComplete((providerResult, throwable) -> {
                        if (throwable != null) {
                            resultFuture.complete(providerThrowableResult(prepared, unwrap(throwable)));
                            return;
                        }
                        if (providerResult == null) {
                            resultFuture.complete(failureResult(prepared, SendStatus.FAILED, SendStage.CONFIRM,
                                    SendFailureType.CLIENT_ERROR, "provider returned null send result"));
                            return;
                        }
                        resultFuture.complete(mapProviderResult(
                                prepared, providerResult, SendReliabilityInfo.disabled()));
                    });
            return resultFuture;
        } catch (RuntimeException exception) {
            resultFuture.complete(providerThrowableResult(prepared, exception));
            return resultFuture;
        }
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
            String description) {
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
                SendReliabilityInfo.disabled());
    }

    private SendResult providerThrowableResult(PreparedMessageSend prepared, Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return failureResult(prepared, SendStatus.UNKNOWN, SendStage.CONFIRM,
                    SendFailureType.TIMEOUT, "send confirmation timeout after " + prepared.confirmTimeout());
        }
        return failureResult(prepared, SendStatus.UNKNOWN, SendStage.CONFIRM,
                SendFailureType.UNKNOWN_OUTCOME,
                throwable == null ? "unknown provider failure" : throwable.getMessage());
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
