package com.xjtu.iron.foundation.core.text;

/**
 * 提供面向技术日志的基础脱敏能力。
 *
 * <p>该类不替代完整的数据安全组件，只提供手机号、邮箱和通用区间遮盖。</p>
 */
public final class TextMasker {

    private TextMasker() {
    }

    /**
     * 对手机号中间四位进行遮盖。
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return maskRange(mobile, 3, mobile.length() - 4, '*');
    }

    /**
     * 对邮箱用户名部分进行遮盖，并保留首字符和域名。
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * 遮盖指定的字符索引区间，区间采用左闭右开语义。
     */
    public static String maskRange(String value, int startInclusive, int endExclusive, char maskChar) {
        if (value == null) {
            return null;
        }
        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > value.length()) {
            throw new IllegalArgumentException("invalid mask range");
        }
        StringBuilder result = new StringBuilder(value);
        for (int index = startInclusive; index < endExclusive; index++) {
            result.setCharAt(index, maskChar);
        }
        return result.toString();
    }
}
