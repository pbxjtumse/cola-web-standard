package com.xjtu.iron.foundation.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 提供时区边界转换。
 */
public final class ZoneSupport {

    private ZoneSupport() {
    }

    public static ZonedDateTime atZone(Instant instant, ZoneId zoneId) {
        if (instant == null || zoneId == null) {
            throw new IllegalArgumentException("instant and zoneId must not be null");
        }
        return instant.atZone(zoneId);
    }

    public static Instant toInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        if (localDateTime == null || zoneId == null) {
            throw new IllegalArgumentException("localDateTime and zoneId must not be null");
        }
        return localDateTime.atZone(zoneId).toInstant();
    }
}
