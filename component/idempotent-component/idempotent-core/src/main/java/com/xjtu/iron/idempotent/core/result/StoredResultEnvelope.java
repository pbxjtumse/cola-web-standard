package com.xjtu.iron.idempotent.core.result;

import com.xjtu.iron.idempotent.api.result.IdempotencyResultPolicyType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * result_payload 的内部协议。
 *
 * <p>格式固定为 {@code IR1|SNAPSHOT|<base64>} 或 {@code IR1|REFERENCE|<base64>}。
 * 把 ResultPolicy 类型写进 envelope，是为了防止历史 SNAPSHOT 被新的 REFERENCE 调用错误解释，反之亦然。</p>
 */
public final class StoredResultEnvelope {

    private static final String PREFIX = "IR1|";

    private StoredResultEnvelope() {
    }

    /**
     * 第一次真实执行成功时，把 ResultPolicy.capture() 的结果封装成可持久化字符串。
     */
    public static String encode(IdempotencyResultPolicyType type, String value) {
        if (value == null) {
            return null;
        }
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return PREFIX + type.name() + "|" + encoded;
    }

    /**
     * 重复请求命中 SUCCESS 时解析历史 payload。协议不匹配直接失败，不猜测旧格式。
     */
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

        IdempotencyResultPolicyType type = IdempotencyResultPolicyType.valueOf(payload.substring(PREFIX.length(), typeEnd));
        String value = new String(Base64.getDecoder().decode(payload.substring(typeEnd + 1)), StandardCharsets.UTF_8);
        return new Decoded(type, value);
    }

    public record Decoded(IdempotencyResultPolicyType type, String value) {
    }
}
