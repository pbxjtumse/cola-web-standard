package com.xjtu.iron.foundation.time;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * 集中定义稳定、线程安全的时间格式器。
 */
public final class TimeFormats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_TIME;
    public static final DateTimeFormatter INSTANT = DateTimeFormatter.ISO_INSTANT;
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter COMPACT_DATE =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter COMPACT_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);

    private TimeFormats() {
    }
}
