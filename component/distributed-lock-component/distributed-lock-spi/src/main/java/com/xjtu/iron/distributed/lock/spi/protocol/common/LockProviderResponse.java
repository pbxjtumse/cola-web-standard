package com.xjtu.iron.distributed.lock.spi.protocol.common;

import com.xjtu.iron.distributed.lock.spi.LockProviderError;

/**
 * Provider 响应通用接口。
 *
 * @param <S> Provider 操作状态枚举类型。
 */
public interface LockProviderResponse<S> {

    /** Provider 操作状态。 */
    S getStatus();

    /** Provider 异常信息。 */
    LockProviderError getProviderError();

    default Throwable getError() {
        LockProviderError lockProviderError = getProviderError();
        return lockProviderError == null ? null : lockProviderError.getCause();
    }

    default String getMessage() {
        LockProviderError lockProviderError = getProviderError();
        return lockProviderError == null ? null : lockProviderError.getMessage();
    }

    default boolean hasProviderError() {
        LockProviderError lockProviderError = getProviderError();
        return lockProviderError != null && lockProviderError.isPresent();
    }
}
