package com.xjtu.iron.foundation.codec;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 提供 URL 查询参数编解码能力。
 */
public final class UrlCodec {

    private UrlCodec() {
    }

    public static String encode(String value) {
        return encode(value, StandardCharsets.UTF_8);
    }

    public static String encode(String value, Charset charset) {
        return value == null ? null : URLEncoder.encode(value, charset);
    }

    public static String decode(String value) {
        return decode(value, StandardCharsets.UTF_8);
    }

    public static String decode(String value, Charset charset) {
        return value == null ? null : URLDecoder.decode(value, charset);
    }
}
