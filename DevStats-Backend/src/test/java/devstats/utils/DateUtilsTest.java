package devstats.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    void startOfTodayUtcReturnsBeginningOfDay() {
        Instant startOfDay = DateUtils.startOfTodayUtc();
        LocalDate expected = LocalDate.now(ZoneOffset.UTC);
        LocalDate actual = startOfDay.atZone(ZoneOffset.UTC).toLocalDate();
        assertEquals(expected, actual);
    }

    @Test
    void startOfTodayUtcReturnsExactMidnight() {
        Instant startOfDay = DateUtils.startOfTodayUtc();
        assertEquals(0, startOfDay.atZone(ZoneOffset.UTC).toLocalTime().toNanoOfDay());
    }

    @Test
    void daysAgoReturnsPastInstant() {
        Instant threeDaysAgo = DateUtils.daysAgo(3);
        assertTrue(threeDaysAgo.isBefore(Instant.now()));
        assertTrue(threeDaysAgo.isAfter(Instant.now().minusSeconds(4 * 86400)));
    }

    @Test
    void daysAgoZeroReturnsApproximatelyNow() {
        Instant zeroDaysAgo = DateUtils.daysAgo(0);
        long diff = Math.abs(Instant.now().getEpochSecond() - zeroDaysAgo.getEpochSecond());
        assertTrue(diff < 5);
    }

    @Test
    void isAfterReturnsTrueForFutureInstant() {
        Instant past = Instant.now().minusSeconds(86400);
        assertTrue(DateUtils.isAfter(Instant.now().toString(), past));
    }

    @Test
    void isAfterReturnsFalseForPastInstant() {
        Instant future = Instant.now().plusSeconds(86400);
        assertFalse(DateUtils.isAfter(Instant.now().toString(), future));
    }

    @Test
    void isAfterReturnsFalseForBlankString() {
        assertFalse(DateUtils.isAfter("", Instant.now()));
        assertFalse(DateUtils.isAfter("  ", Instant.now()));
    }

    @Test
    void isAfterReturnsFalseForNull() {
        assertFalse(DateUtils.isAfter(null, Instant.now()));
    }

    @Test
    void nowMillisReturnsPositiveValue() {
        assertTrue(DateUtils.nowMillis() > 0);
    }
}
