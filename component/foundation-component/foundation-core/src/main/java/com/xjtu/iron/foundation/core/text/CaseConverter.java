package com.xjtu.iron.foundation.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 在常用标识符命名格式之间转换。
 */
public final class CaseConverter {

    private CaseConverter() {
    }

    /**
     * 将标识符从源格式转换为目标格式。
     */
    public static String convert(String value, CaseFormat source, CaseFormat target) {
        if (value == null) {
            return null;
        }
        if (source == null || target == null) {
            throw new IllegalArgumentException("source and target must not be null");
        }
        if (source == target || value.isEmpty()) {
            return value;
        }

        List<String> words = splitWords(value, source);
        return joinWords(words, target);
    }

    private static List<String> splitWords(String value, CaseFormat source) {
        return switch (source) {
            case LOWER_UNDERSCORE, UPPER_UNDERSCORE -> splitByDelimiter(value, "_");
            case LOWER_HYPHEN -> splitByDelimiter(value, "-");
            case LOWER_CAMEL, UPPER_CAMEL -> splitCamel(value);
        };
    }

    private static List<String> splitByDelimiter(String value, String delimiter) {
        String[] parts = value.split(java.util.regex.Pattern.quote(delimiter));
        List<String> words = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part.toLowerCase(Locale.ROOT));
            }
        }
        return words;
    }

    private static List<String> splitCamel(String value) {
        String separated = value.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return splitByDelimiter(separated, " ");
    }

    private static String joinWords(List<String> words, CaseFormat target) {
        if (words.isEmpty()) {
            return "";
        }
        return switch (target) {
            case LOWER_UNDERSCORE -> String.join("_", words);
            case UPPER_UNDERSCORE -> String.join("_", words).toUpperCase(Locale.ROOT);
            case LOWER_HYPHEN -> String.join("-", words);
            case LOWER_CAMEL -> toCamel(words, false);
            case UPPER_CAMEL -> toCamel(words, true);
        };
    }

    private static String toCamel(List<String> words, boolean capitalizeFirst) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.size(); index++) {
            String word = words.get(index);
            if (index == 0 && !capitalizeFirst) {
                result.append(word);
            } else {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
        }
        return result.toString();
    }
}
