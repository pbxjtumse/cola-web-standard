package com.xjtu.iron.foundation.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * 提供严格字符集转换，遇到非法字节时拒绝静默替换。
 */
public final class CharsetSupport {

    private CharsetSupport() {
    }

    public static byte[] utf8(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }

    public static String utf8(byte[] value) {
        return value == null ? null : decodeStrict(value, StandardCharsets.UTF_8);
    }

    public static String decodeStrict(byte[] value, Charset charset) {
        if (value == null) {
            return null;
        }
        try {
            CharBuffer decoded = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(value));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("input contains invalid " + charset.name() + " bytes", exception);
        }
    }
}
