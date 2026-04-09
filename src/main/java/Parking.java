import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class Parking implements CSVEncodable, TablePrintable {
    private int id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double price;
    private String email;
    private int parkId;
    private int rateId;
    private Park park;

    public Parking(int id, LocalDateTime startTime, LocalDateTime endTime, double price, String email, int parkId,
            int rateId) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.email = email;
        this.parkId = parkId;
        this.rateId = rateId;
    }

    public Park park() {
        return park;
    }

    public void setPark(Park park) {
        this.park = park;
    }

    public int id() {
        return id;
    }

    public int parkId() {
        return parkId;
    }

    public String email() {
        return email;
    }

    // funkcija toCSV nepieņem nevienu vērtību un atgriež String tipa vērtību toCSV
    public String toCSV() {
        return id + "," + startTime + "," + endTime + "," + price + "," + email + "," + parkId + ","
                + rateId + "\n";
    }

    // funkcija toTableRow pieņem List<Integer> tipa vērtību widths un atgriež
    // String tipa vērtību tableRow
    public String toTableRow(List<Integer> widths) {
        String formatString = widths.stream()
                .map(w -> "%-" + w + "s")
                .collect(Collectors.joining(" | ", "| ", " |"));

        return String.format(formatString, park.address(),
                startTime.format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")),
                endTime.format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")), price);
    }

    // funkcija fromCSV pieņem String tipa vērtību csvData un atgriež Rate tipa
    // vērtību noCSV
    public static Parking fromCSV(String csvdata) throws Exception {
        // Sadala saņemto teksta rindu masīvā, izmantojot komatu kā atdalītāju
        String[] fields = csvdata.split(",");

        // Pārbauda, vai rindā ir pietiekami daudz datu lauku, lai izveidotu objektu
        if (fields.length < 7) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected 7");
        }

        // return id + "," + startTime + "," + endTime + "," + price + "," + email + ","
        // + parkId + ","
        // + rateId + "\n";

        // Konvertē teksta vērtības uz atbilstošajiem datu tipiem
        int id = Integer.valueOf(fields[0]);
        LocalDateTime startTime = LocalDateTime.parse(fields[1], DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"));
        LocalDateTime endTime = LocalDateTime.parse(fields[2], DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy"));
        double price = Double.valueOf(fields[3]);
        String email = fields[4];
        int parkId = Integer.valueOf(fields[5]);
        int rateId = Integer.valueOf(fields[6]);

        return new Parking(id, startTime, endTime, price, email, parkId, rateId);
    }
}
