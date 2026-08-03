package com.xjtu.iron.foundation.id.api;

/** ID 生成器无法继续安全生成标识时抛出的统一异常。 */
public class IdGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdGenerationException(String message) {
        super(message);
    }

    public IdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
