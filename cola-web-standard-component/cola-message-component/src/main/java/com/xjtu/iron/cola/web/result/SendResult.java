package com.xjtu.iron.cola.web.result;

import com.xjtu.iron.cola.web.enums.SendFailTypeEnum;
import com.xjtu.iron.cola.web.enums.SendStatusEnum;
import com.xjtu.iron.cola.web.enums.SendStatusEnumEnum;
import lombok.Getter;

/**
 * 成功 / 失败 / 不确定
 * 失败的语义类型
 * 原始异常
 * 是否可重试（可推导）
 * @author pangbo
 * @date 2025/12/17
 */
@Getter
public final class SendResult {

    private final SendStatusEnum status;
    private final SendFailTypeEnum failType;
    private final Throwable cause;

    private SendResult(SendStatusEnum status,
                       SendFailTypeEnum failType,
                       Throwable cause) {
        this.status = status;
        this.failType = failType;
        this.cause = cause;
    }

    public static SendResult success() {
        return new SendResult(SendStatusEnum.SUCCESS, null, null);
    }

    public static SendResult fail(SendFailTypeEnum type, Throwable cause) {
        return new SendResult(SendStatusEnum.FAIL, type, cause);
    }

    public static SendResult clientFail(Throwable cause) {
        return fail(SendFailTypeEnum.CLIENT_FAIL, cause);
    }

    public static SendResult brokerReject(Throwable cause) {
        return fail(SendFailTypeEnum.BROKER_REJECT, cause);
    }

    public static SendResult uncertain(Throwable cause) {
        return new SendResult(
                SendStatusEnum.UNCERTAIN,
                SendFailTypeEnum.UNCERTAIN,
                cause
        );
    }
}


