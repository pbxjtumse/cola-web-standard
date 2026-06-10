package com.xjtu.iron.concurrency.api.error;

/**
 * 应用侧错误信息。
 *
 * <p>
 * 并行组件不定义业务异常，也不解释业务错误码。
 * 该对象只是用于承载应用侧传入的 errorCode、sceneCode 和 message。
 * </p>
 */
public class ApplicationErrorInfo {

    /**
     * 应用侧错误码。
     *
     * <p>
     * 例如 ACCOUNT_STATUS_INVALID、ORDER_NOT_FOUND、PAYMENT_POSTING_FAILED。
     * 该字段由业务系统定义，并行组件只负责透传。
     * </p>
     */
    private String errorCode;

    /**
     * 应用侧场景码。
     *
     * <p>
     * 例如 PAYMENT_POSTING、USER_PROFILE_QUERY、COUPON_DELIVERY。
     * 该字段可以用于后续补偿系统识别业务场景。
     * </p>
     */
    private String sceneCode;

    /**
     * 应用侧错误消息。
     */
    private String message;

    public static ApplicationErrorInfo none() {
        return new ApplicationErrorInfo();
    }

    public static ApplicationErrorInfo of(String errorCode, String sceneCode, String message) {
        ApplicationErrorInfo info = new ApplicationErrorInfo();
        info.errorCode = errorCode;
        info.sceneCode = sceneCode;
        info.message = message;
        return info;
    }

    public ApplicationErrorInfo copy() {
        return of(errorCode, sceneCode, message);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getSceneCode() {
        return sceneCode;
    }

    public void setSceneCode(String sceneCode) {
        this.sceneCode = sceneCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}