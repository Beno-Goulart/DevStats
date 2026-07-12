package devstats.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class DateUtils {

    private DateUtils() {
    }

    public static Instant startOfTodayUtc() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    public static boolean isAfter(String isoDateTime, Instant threshold) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return false;
        }
        return Instant.parse(isoDateTime).isAfter(threshold);
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }
}
