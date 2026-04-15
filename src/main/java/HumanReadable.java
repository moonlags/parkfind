import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

public class HumanReadable {
    public static String formatInterval(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start))
            throw new IllegalArgumentException("end must be after start");

        Period period = Period.between(start.toLocalDate(), end.toLocalDate());
        LocalDateTime mid = start.plus(period);
        Duration duration = Duration.between(mid, end);

        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long seconds = duration.minusHours(hours).minusMinutes(minutes).getSeconds();

        StringBuilder sb = new StringBuilder();
        append(sb, years, "g");
        append(sb, months, "m");
        append(sb, days, "d");
        append(sb, hours, "st");
        append(sb, minutes, "min");
        append(sb, seconds, "sek");

        String result = sb.length() == 0 ? "0 sek" : sb.toString().trim();
        // make nicer: replace last comma with " and"
        int lastComma = result.lastIndexOf(',');
        if (lastComma != -1)
            result = result.substring(0, lastComma) + " un" + result.substring(lastComma + 1);
        return result;
    }

    private static void append(StringBuilder sb, long value, String unit) {
        if (value <= 0)
            return;
        if (sb.length() > 0)
            sb.append(", ");
        sb.append(value).append(' ').append(unit);
    }
}
