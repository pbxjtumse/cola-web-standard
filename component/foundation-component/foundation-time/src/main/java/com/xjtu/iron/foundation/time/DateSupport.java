package com.xjtu.iron.foundation.time;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 提供不包含业务日历语义的自然日期计算。
 */
public final class DateSupport {

    private DateSupport() {
    }

    public static LocalDate firstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public static LocalDate lastDayOfMonth(LocalDate date) {
        return YearMonth.from(date).atEndOfMonth();
    }

    public static DateRange monthRange(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }
}
