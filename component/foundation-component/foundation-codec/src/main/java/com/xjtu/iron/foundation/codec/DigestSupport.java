package com.xjtu.iron.foundation.codec;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 提供不可逆内容摘要和常量时间比较。
 *
 * <p>本类不用于密码存储；密码必须使用专门的自适应哈希算法和安全组件。</p>
 */
public final class DigestSupport {

    private DigestSupport() {
    }

    public static byte[] digest(byte[] value, DigestAlgorithm algorithm) {
        if (value == null) {
            return null;
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        try {
            return MessageDigest.getInstance(algorithm.getJcaName()).digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JCA digest algorithm is unavailable: " + algorithm, exception);
        }
    }

    public static String digestHex(String value, DigestAlgorithm algorithm, Charset charset) {
        return value == null ? null : HexSupport.encodeLower(digest(value.getBytes(charset), algorithm));
    }

    public static String sha256Hex(String value) {
        return value == null ? null : DigestUtils.sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha512Hex(String value) {
        return value == null ? null : DigestUtils.sha512Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean constantTimeEquals(byte[] left, byte[] right) {
        return left != null && right != null && MessageDigest.isEqual(left, right);
    }
}
