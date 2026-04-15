import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Util {
    public static boolean isDateAllowedByWeekdays(LocalDate date, byte weekdays) {
        int dowIndex = date.getDayOfWeek().getValue();
        return (weekdays & (1 << (dowIndex - 1))) != 0;
    }

    public static long hoursBetweenDates(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toSeconds();
        return (long) Math.ceil(minutes / 3600.0);
    }

    public static LocalDateTime calculateWindowEnd(Rate rate, LocalDateTime cursor, LocalDateTime endTime) {
        LocalDateTime end;
        if (rate.startTime().equals(rate.endTime())) {
            end = endTime;
        } else if (!rate.startTime().isAfter(rate.endTime())) {
            end = cursor.toLocalDate().atTime(rate.endTime());
        } else {
            if (cursor.toLocalTime().isBefore(rate.endTime())) {
                end = cursor.toLocalDate().atTime(rate.endTime());
            } else {
                end = cursor.toLocalDate().plusDays(1).atTime(rate.endTime());
            }
        }
        return end.isAfter(endTime) ? endTime : end;
    }
}
