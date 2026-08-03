package com.xjtu.iron.foundation.codec;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 提供基本类型和字节数组转换能力。
 */
public final class ByteSupport {

    private ByteSupport() {
    }

    public static byte[] fromLong(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    public static long toLong(byte[] value) {
        requireLength(value, Long.BYTES);
        return ByteBuffer.wrap(value).getLong();
    }

    public static byte[] fromInt(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    public static int toInt(byte[] value) {
        requireLength(value, Integer.BYTES);
        return ByteBuffer.wrap(value).getInt();
    }

    public static byte[] concat(byte[]... arrays) {
        if (arrays == null) {
            return null;
        }
        int length = Arrays.stream(arrays).mapToInt(array -> array == null ? 0 : array.length).sum();
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }
        }
        return result;
    }

    private static void requireLength(byte[] value, int expected) {
        if (value == null || value.length != expected) {
            throw new IllegalArgumentException("byte array length must be " + expected);
        }
    }
}
