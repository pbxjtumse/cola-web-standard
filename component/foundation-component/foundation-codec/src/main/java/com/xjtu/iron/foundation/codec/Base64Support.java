package com.xjtu.iron.foundation.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 提供标准 Base64 和 URL Safe Base64 编解码能力。
 */
public final class Base64Support {

    private Base64Support() {
    }

    public static String encode(byte[] value) {
        return value == null ? null : Base64.getEncoder().encodeToString(value);
    }

    public static byte[] decode(String value) {
        return value == null ? null : Base64.getDecoder().decode(value);
    }

    public static String encodeText(String value) {
        return encodeText(value, StandardCharsets.UTF_8);
    }

    public static String encodeText(String value, Charset charset) {
        return value == null ? null : encode(value.getBytes(charset));
    }

    public static String decodeText(String value) {
        return decodeText(value, StandardCharsets.UTF_8);
    }

    public static String decodeText(String value, Charset charset) {
        return value == null ? null : new String(decode(value), charset);
    }

    public static String encodeUrlSafe(byte[] value, boolean withoutPadding) {
        if (value == null) {
            return null;
        }
        Base64.Encoder encoder = withoutPadding ? Base64.getUrlEncoder().withoutPadding() : Base64.getUrlEncoder();
        return encoder.encodeToString(value);
    }

    public static byte[] decodeUrlSafe(String value) {
        return value == null ? null : Base64.getUrlDecoder().decode(value);
    }
}
