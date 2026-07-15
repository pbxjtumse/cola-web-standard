package com.xjtu.iron.distributed.lock.core.spi;

import java.util.Optional;

/**
 * Provider 层异常信息。
 *
 * <p>该对象只描述 Provider 操作异常，不表示业务 callback 异常。</p>
 */
public final class LockProviderError {

    private static final LockProviderError NONE = new LockProviderError(null, null);

    private final Throwable cause;
    private final String message;

    private LockProviderError(Throwable cause, String message) {
        this.cause = cause;
        this.message = normalize(message);
    }

    public static LockProviderError none() {
        return NONE;
    }

    public static LockProviderError of(Throwable cause) {
        if (cause == null) {
            return NONE;
        }
        return new LockProviderError(cause, cause.getMessage());
    }

    public static LockProviderError of(Throwable cause, String message) {
        if (cause == null && normalize(message) == null) {
            return NONE;
        }
        return new LockProviderError(cause, message);
    }

    public Optional<Throwable> cause() {
        return Optional.ofNullable(cause);
    }

    public Throwable getCause() {
        return cause;
    }

    public String message() {
        return message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isPresent() {
        return cause != null || message != null;
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
