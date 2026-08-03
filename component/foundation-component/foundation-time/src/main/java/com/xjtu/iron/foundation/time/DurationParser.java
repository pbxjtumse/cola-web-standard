package com.xjtu.iron.foundation.time;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 ISO-8601 或简写形式的持续时间。
 *
 * <p>支持 {@code 100ms}、{@code 5s}、{@code 3m}、{@code 2h}、{@code 1d} 和 {@code PT5S}。</p>
 */
public final class DurationParser {

    /** 简写持续时间的解析规则。 */
    private static final Pattern SIMPLE = Pattern.compile("^([0-9]+)(ms|s|m|h|d)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("duration value must not be blank");
        }
        String normalized = value.trim();
        if (normalized.toUpperCase(Locale.ROOT).startsWith("P")) {
            return Duration.parse(normalized.toUpperCase(Locale.ROOT));
        }

        Matcher matcher = SIMPLE.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unsupported duration value: " + value);
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalStateException("unreachable duration unit");
        };
    }
}
