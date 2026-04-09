import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;

public class Util {
    public static boolean isDateAllowedByWeekdays(LocalDate date, byte weekdays) {
        int dowIndex = date.getDayOfWeek().getValue();
        return (weekdays & (1 << (dowIndex - 1))) != 0;
    }

    public static long hoursBetweenDates(LocalDateTime start, LocalDateTime end) {
        long hours = ChronoUnit.HOURS.between(start, end);
        if (hours == 0)
            hours++;
        return hours;
    }
}
