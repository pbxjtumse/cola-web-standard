package com.xjtu.iron.foundation.codec;

import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * 提供快速完整性校验值计算。
 *
 * <p>Checksum 用于检测意外损坏，不具备密码学抗碰撞能力。</p>
 */
public final class ChecksumSupport {

    private ChecksumSupport() {
    }

    public static long crc32(byte[] value) {
        return calculate(value, new CRC32());
    }

    public static long adler32(byte[] value) {
        return calculate(value, new Adler32());
    }

    private static long calculate(byte[] value, Checksum checksum) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        checksum.update(value, 0, value.length);
        return checksum.getValue();
    }
}
