import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

public class HumanReadable {
    public static String formatInterval(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start)) {
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }

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
        append(sb, years, "gadus");
        append(sb, months, "menēšus");
        append(sb, days, "dienas");
        append(sb, hours, "stundas");
        append(sb, minutes, "minutes");
        append(sb, seconds, "sekundes");

        String result = sb.length() == 0 ? "0 sekundes" : sb.toString().trim();
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