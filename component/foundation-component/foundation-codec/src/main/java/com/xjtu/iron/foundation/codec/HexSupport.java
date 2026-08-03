package com.xjtu.iron.foundation.codec;

import java.util.HexFormat;

/**
 * 提供十六进制编解码能力。
 */
public final class HexSupport {

    /** 小写十六进制格式器。 */
    private static final HexFormat LOWER = HexFormat.of();
    /** 大写十六进制格式器。 */
    private static final HexFormat UPPER = HexFormat.of().withUpperCase();

    private HexSupport() {
    }

    public static String encodeLower(byte[] value) {
        return value == null ? null : LOWER.formatHex(value);
    }

    public static String encodeUpper(byte[] value) {
        return value == null ? null : UPPER.formatHex(value);
    }

    public static byte[] decode(String value) {
        return value == null ? null : LOWER.parseHex(value);
    }
}
