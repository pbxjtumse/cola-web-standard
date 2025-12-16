package com.xjtu.iron.cola.web.result;

import com.xjtu.iron.cola.web.enums.SendFailTypeEnum;

public class SendResult {

    private boolean success;

    private SendFailTypeEnum sendFailTypeEnum;

    private String messageId;

    private Throwable exception;
}
