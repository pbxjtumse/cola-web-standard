package com.xjtu.iron.distributed.lock.core.fencing;

import com.xjtu.iron.distributed.lock.core.spi.LockProviderError;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * 独立 fencing token Provider 的响应。
 *
 * <p>该响应只描述“发号”结果，不等同于最终 {@code LockResult}。Core 会把
 * {@link FencingTokenStatus#PROVIDER_ERROR} 映射为
 * {@code LockStatus.PROVIDER_ERROR + LockStage.FENCING}。</p>
 */
public final class FencingTokenResponse {

    private final FencingTokenStatus status;
    private final Long token;
    private final LockProviderError providerError;

    private FencingTokenResponse(FencingTokenStatus status, Long token, LockProviderError providerError) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.token = token;
        this.providerError = providerError == null ? LockProviderError.none() : providerError;
        if (status == FencingTokenStatus.ISSUED && (token == null || token <= 0L)) {
            throw new IllegalArgumentException("issued fencing token must be positive");
        }
        if (status != FencingTokenStatus.ISSUED && token != null) {
            throw new IllegalArgumentException("token must be null when status is not ISSUED");
        }
    }

    public static FencingTokenResponse issued(long token) {
        return new FencingTokenResponse(FencingTokenStatus.ISSUED, token, LockProviderError.none());
    }

    /** 兼容旧命名。 */
    public static FencingTokenResponse success(long token) {
        return issued(token);
    }

    public static FencingTokenResponse notSupported(String message) {
        return new FencingTokenResponse(FencingTokenStatus.NOT_SUPPORTED, null,
                LockProviderError.of(null, message));
    }

    public static FencingTokenResponse failed(Throwable error) {
        return new FencingTokenResponse(FencingTokenStatus.PROVIDER_ERROR, null,
                LockProviderError.of(error));
    }

    public static FencingTokenResponse failed(Throwable error, String message) {
        return new FencingTokenResponse(FencingTokenStatus.PROVIDER_ERROR, null,
                LockProviderError.of(error, message));
    }

    public FencingTokenStatus getStatus() {
        return status;
    }

    public boolean isIssued() {
        return status == FencingTokenStatus.ISSUED;
    }

    /** 兼容旧命名。 */
    public boolean isSuccess() {
        return isIssued();
    }

    public OptionalLong token() {
        return token == null ? OptionalLong.empty() : OptionalLong.of(token);
    }

    public boolean hasProviderError() {
        return providerError.isPresent();
    }

    public LockProviderError getProviderError() {
        return providerError;
    }

    public Throwable getError() {
        return providerError.getCause();
    }

    public String getMessage() {
        return providerError.getMessage();
    }
}
