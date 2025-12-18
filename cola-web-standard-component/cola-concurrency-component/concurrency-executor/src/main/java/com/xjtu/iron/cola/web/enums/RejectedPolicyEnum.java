package com.xjtu.iron.cola.web.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    // 缓存映射，提高查找性能
    private static final Map<String, RejectedPolicyEnum> CODE_MAP = new ConcurrentHashMap<>();
    static {
        Arrays.stream(RejectedPolicyEnum.values())
                .forEach(policy -> CODE_MAP.put(policy.getCode(), policy));
    }

    /**
     * 根据code获取枚举
     * @param code 枚举code
     * @return 对应的枚举，如果不存在返回null
     */
    public static RejectedPolicyEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据code获取枚举，如果不存在抛出异常
     * @param code 枚举code
     * @return 对应的枚举
     * @throws IllegalArgumentException 如果枚举不存在
     */
    public static RejectedPolicyEnum getByCodeOrThrow(String code) {
        RejectedPolicyEnum policy = CODE_MAP.get(code);
        if (policy == null) {
            throw new IllegalArgumentException("Invalid RejectedPolicyEnum code: " + code);
        }
        return policy;
    }
}
