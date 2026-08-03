package com.xjtu.iron.foundation.codec;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 表示由内容计算得到的稳定指纹。
 */
public final class ContentFingerprint {

    /** 计算当前指纹使用的摘要算法。 */
    private final DigestAlgorithm algorithm;
    /** 摘要结果的小写十六进制表示。 */
    private final String hexValue;

    private ContentFingerprint(DigestAlgorithm algorithm, String hexValue) {
        this.algorithm = algorithm;
        this.hexValue = hexValue;
    }

    public static ContentFingerprint of(byte[] content, DigestAlgorithm algorithm) {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return new ContentFingerprint(algorithm, HexSupport.encodeLower(DigestSupport.digest(content, algorithm)));
    }

    public static ContentFingerprint ofText(String content, DigestAlgorithm algorithm) {
        Objects.requireNonNull(content, "content must not be null");
        return of(content.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    public DigestAlgorithm getAlgorithm() { return algorithm; }
    public String getHexValue() { return hexValue; }

    @Override
    public String toString() {
        return algorithm.name() + ':' + hexValue;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ContentFingerprint other
                && algorithm == other.algorithm
                && hexValue.equals(other.hexValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, hexValue);
    }
}
