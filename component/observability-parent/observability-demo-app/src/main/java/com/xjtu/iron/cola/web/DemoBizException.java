package com.xjtu.iron;


/**
 * Demo 业务异常。
 *
 * <p>模拟未来业务项目自己的异常体系。</p>
 */
public class DemoBizException extends RuntimeException {

    private final String code;

    public DemoBizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
