package com.xjtu.iron.foundation.id.nanoid;

import com.xjtu.iron.foundation.id.api.StringIdGenerator;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * 使用加密强随机数和无模偏差采样生成 URL 友好短标识。
 *
 * <p>默认使用 64 字符字母表和 21 位长度，对应 Nano ID 的主流安全参数。</p>
 */
public final class NanoIdStringIdGenerator implements StringIdGenerator {

    private final char[] alphabet;
    private final int size;
    private final SecureRandom secureRandom;
    private final int mask;
    private final int step;

    public NanoIdStringIdGenerator() {
        this(NanoIdOptions.defaults(), new SecureRandom());
    }

    public NanoIdStringIdGenerator(NanoIdOptions options) {
        this(options, new SecureRandom());
    }

    public NanoIdStringIdGenerator(NanoIdOptions options, SecureRandom secureRandom) {
        NanoIdOptions actualOptions = Objects.requireNonNull(
                options,
                "options must not be null"
        );
        this.alphabet = actualOptions.getAlphabet().toCharArray();
        this.size = actualOptions.getSize();
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom must not be null"
        );
        this.mask = calculateMask(alphabet.length);
        this.step = (int) Math.ceil(1.6D * mask * size / alphabet.length);
    }

    @Override
    public String nextId() {
        if (alphabet.length == 1) {
            return String.valueOf(alphabet[0]).repeat(size);
        }

        StringBuilder id = new StringBuilder(size);
        byte[] bytes = new byte[step];
        while (id.length() < size) {
            secureRandom.nextBytes(bytes);
            for (byte randomByte : bytes) {
                int alphabetIndex = randomByte & mask;
                if (alphabetIndex < alphabet.length) {
                    id.append(alphabet[alphabetIndex]);
                    if (id.length() == size) {
                        return id.toString();
                    }
                }
            }
        }
        return id.toString();
    }

    private static int calculateMask(int alphabetSize) {
        if (alphabetSize == 1) {
            return 0;
        }
        int bits = 32 - Integer.numberOfLeadingZeros(alphabetSize - 1);
        return (1 << bits) - 1;
    }
}
