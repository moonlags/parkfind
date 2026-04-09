import java.time.LocalDate;

public class Util {
    public static boolean isDateAllowedByWeekdays(LocalDate date, byte weekdays) {
        int dowIndex = date.getDayOfWeek().getValue();
        return (weekdays & (1 << dowIndex)) != 0;
    }
}
