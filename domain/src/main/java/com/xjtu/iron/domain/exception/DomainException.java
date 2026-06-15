package com.xjtu.iron.domain.exception;


/**
 * 领域异常示例。
 *
 * <p>
 * 这个类属于业务工程，不属于 concurrency-component。
 * </p>
 */
public class DomainException extends RuntimeException {

    /**
     * 业务错误码。
     */
    private final String errorCode;

    /**
     * 业务场景码。
     */
    private final String sceneCode;

    public DomainException(String errorCode, String sceneCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.sceneCode = sceneCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getSceneCode() {
        return sceneCode;
    }
}
