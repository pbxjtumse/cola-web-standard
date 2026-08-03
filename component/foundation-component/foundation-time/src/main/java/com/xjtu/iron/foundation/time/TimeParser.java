package com.xjtu.iron.foundation.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 提供带清晰异常信息的时间解析能力。
 */
public final class TimeParser {

    private TimeParser() {
    }

    public static LocalDate parseDate(String value, DateTimeFormatter formatter) {
        try {
            return LocalDate.parse(value, formatter);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid date value: " + value, exception);
        }
    }

    public static LocalDateTime parseDateTime(String value, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid date-time value: " + value, exception);
        }
    }

    public static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid instant value: " + value, exception);
        }
    }
}
