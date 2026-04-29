package com.xjtu.iron.cola.web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author pbxjtu
 */

@Getter
@AllArgsConstructor
public enum RetryPolicyEnum {
    MAX_RETRY_COUNT("maxRetryCount","3"),
    BACKOFF_STRATEGY("backoffStrategy","3");
    private final String code;
    private final String value;



}
