package com.xjtu.iron.cola.web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 线程池拒绝策略
 * @author pbxjtu
 */

@AllArgsConstructor
@Getter
public enum RejectedPolicyEnum {
    CALLER_RUNS("CALLER_RUNS","CALLER_RUNS"),
    DISCARD("DISCARD","DISCARD"),
    DISCARD_OLDEST("DISCARD_OLDEST","DISCARD_OLDEST"),
    ABORT("ABORT","ABORT");
    private final String code;
    private final String value;
}
