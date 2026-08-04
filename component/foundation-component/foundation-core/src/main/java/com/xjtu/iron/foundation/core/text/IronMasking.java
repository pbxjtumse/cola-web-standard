package com.xjtu.iron.foundation.core.text;

/**
 * 常见日志输出脱敏工具。
 *
 * <p>该类只用于技术日志、事件摘要和错误描述中的基础脱敏，不承担安全合规策略、密钥管理或审计策略。</p>
 */
public final class IronMasking {

    private IronMasking() {
    }

    /**
     * 对手机号进行基础脱敏，例如 13812345678 -> 138****5678。
     *
     * @param mobile 手机号或类似长度的字符串
     * @return 脱敏结果
     */
    public static String maskMobile(String mobile) {
        return maskMiddle(mobile, 3, 4, "****");
    }

    /**
     * 对邮箱本地部分进行基础脱敏。
     *
     * @param email 邮箱地址
     * @return 脱敏结果
     */
    public static String maskEmail(String email) {
        if (IronStrings.isBlank(email)) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 保留前后若干字符，中间用掩码替换。
     *
     * @param value 原始字符串
     * @param prefix 保留前缀长度
     * @param suffix 保留后缀长度
     * @param mask 掩码文本
     * @return 脱敏结果
     */
    public static String maskMiddle(String value, int prefix, int suffix, String mask) {
        if (value == null) {
            return null;
        }
        if (prefix < 0 || suffix < 0) {
            throw new IllegalArgumentException("prefix and suffix must not be negative");
        }
        String actualMask = mask == null ? "***" : mask;
        int length = value.length();
        if (length <= prefix + suffix) {
            return actualMask;
        }
        return value.substring(0, prefix) + actualMask + value.substring(length - suffix);
    }
}
