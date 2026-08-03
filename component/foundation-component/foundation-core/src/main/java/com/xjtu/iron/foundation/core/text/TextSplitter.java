package com.xjtu.iron.foundation.core.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 提供可预测的文本拆分能力。
 */
public final class TextSplitter {

    private TextSplitter() {
    }

    /**
     * 按字面分隔符拆分文本，并忽略空白结果。
     */
    public static List<String> splitAndTrim(String value, String delimiter) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("delimiter must not be empty");
        }

        String[] parts = value.split(Pattern.quote(delimiter), -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String normalized = TextNormalizer.trimToNull(part);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 按固定长度将文本切分为多个片段。
     */
    public static List<String> splitByCodePoints(String value, int chunkSize) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }

        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int remaining = value.codePointCount(start, value.length());
            int count = Math.min(chunkSize, remaining);
            int end = value.offsetByCodePoints(start, count);
            result.add(value.substring(start, end));
            start = end;
        }
        return Collections.unmodifiableList(result);
    }
}
