package com.xjtu.iron.foundation.codec;

/**
 * 定义基础组件允许使用的安全摘要算法。
 */
public enum DigestAlgorithm {
    SHA_256("SHA-256"),
    SHA_384("SHA-384"),
    SHA_512("SHA-512");

    /** Java Cryptography Architecture 使用的标准算法名称。 */
    private final String jcaName;

    DigestAlgorithm(String jcaName) {
        this.jcaName = jcaName;
    }

    public String getJcaName() {
        return jcaName;
    }
}
