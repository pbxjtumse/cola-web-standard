package com.xjtu.iron.idempotent.core.result;

import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicyType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * result_payload 内部协议。
 *
 * <p>格式：{@code IR1|SNAPSHOT|<base64>} 或 {@code IR1|REFERENCE|<base64>}。
 * ResultPolicy 类型进入 envelope，避免相同 payload 被错误地按另一种策略解释。</p>
 */
public final class StoredResultEnvelope {

    private static final String PREFIX = "IR1|";

    private StoredResultEnvelope() {
    }

    public static String encode(IdempotencyResultPolicyType type, String value) {
        if (value == null) {
            return null;
        }
        String encoded = Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
        return PREFIX + type.name() + "|" + encoded;
    }

    public static Decoded decode(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        if (!payload.startsWith(PREFIX)) {
            throw new IllegalArgumentException("invalid stored result envelope prefix");
        }

        int typeEnd = payload.indexOf('|', PREFIX.length());
        if (typeEnd < 0) {
            throw new IllegalArgumentException("invalid stored result envelope");
        }

        String typeText = payload.substring(PREFIX.length(), typeEnd);
        IdempotencyResultPolicyType type = IdempotencyResultPolicyType.valueOf(typeText);
        String base64 = payload.substring(typeEnd + 1);
        String value = new String(
                Base64.getDecoder().decode(base64),
                StandardCharsets.UTF_8);
        return new Decoded(type, value);
    }

    public record Decoded(
            IdempotencyResultPolicyType type,
            String value) {
    }
}
