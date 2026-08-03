package com.xjtu.iron.foundation.core.validation;

import java.util.Objects;

/**
 * 描述一次结构性校验失败。
 */
public final class ValidationViolation {

    /** 发生校验问题的字段或属性名称。 */
    private final String field;
    /** 机器可识别的校验问题编码。 */
    private final String code;
    /** 面向开发人员的校验问题说明。 */
    private final String message;

    public ValidationViolation(String field, String code, String message) {
        this.field = Objects.requireNonNull(field, "field must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public String getField() { return field; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
