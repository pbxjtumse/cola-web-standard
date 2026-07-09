package com.xjtu.iron.distributed.lock.core.fencing;

import java.util.OptionalLong;

/**
 * fencing token 生成响应。
 */
public final class FencingTokenResponse {

    /** 是否成功生成 token。 */
    private final boolean success;

    /** fencing token。 */
    private final Long token;

    /** 异常信息。 */
    private final Throwable error;

    /** 附加消息。 */
    private final String message;

    private FencingTokenResponse(boolean success, Long token, Throwable error, String message) {
        this.success = success;
        this.token = token;
        this.error = error;
        this.message = message;
    }

    public static FencingTokenResponse success(long token) {
        return new FencingTokenResponse(true, token, null, null);
    }

    public static FencingTokenResponse notSupported(String message) {
        return new FencingTokenResponse(false, null, null, message);
    }

    public static FencingTokenResponse failed(Throwable error) {
        return new FencingTokenResponse(false, null, error, error == null ? null : error.getMessage());
    }

    public boolean isSuccess() { return success; }
    public OptionalLong token() { return token == null ? OptionalLong.empty() : OptionalLong.of(token); }
    public Throwable getError() { return error; }
    public String getMessage() { return message; }
}
