package com.xjtu.iron.cola.web.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author pbxjtu
 */

@Getter
@AllArgsConstructor
public enum DeadMessageEnum {
    MESSAGE("Message","Message"),
    THROWABLE("Throwable","Throwable"),
    RETRY_COUNT("retryCount","retryCount"),
    FAILED_AT("failedAt","failedAt");
    private final String code;
    private final String value;
}
