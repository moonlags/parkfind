import java.time.LocalTime;

enum RateType {
    PerHour,
    DayWhole,
    MonthWhole,
    MonthPart,
    DaysMultiple
}

public class Rate implements CSVEncodable {
    private int id;
    private int parkId;
    private AutoType autoType;
    private RateType rateType;
    private float price;
    private LocalTime startTime;
    private LocalTime endTime;
    private int multipleCount;
    private byte weekDays;

    // funkcija Rate pieņem int, int, AutoType, float, String, float tipa vērtības
    // id, parkId, autoType, hourRate, name, fullPrice un neatgriež nekādu vērtību
    public Rate(int id, int parkId, AutoType autoType, RateType rateType, float price, LocalTime startTime,
            LocalTime endTime, int multipleCount, byte weekDays) {
        this.id = id;
        this.parkId = parkId;
        this.autoType = autoType;
        this.rateType = rateType;
        this.price = price;
        this.startTime = startTime;
        this.endTime = endTime;
        this.multipleCount = multipleCount;
        this.weekDays = weekDays;
    }

    // funkcija parkId nepieņem nevienu vērtību un atgriež int tipa vērtību parkId
    public int parkId() {
        return parkId;
    }

    public int id() {
        return id;
    }

    // funkcija toCSV nepieņem nevienu vērtību un atgriež String tipa vērtību
    // csvRinda
    public String toCSV() {
        return id + "," + parkId + "," + autoType.name() + "," + rateType.name() + "," + price + "," + startTime + ","
                + endTime + "," + multipleCount + "," + weekDays + "\n";
    }

    public String toString() {
        // TODO add complex toString, that check rateType and print accordingly
        // Include id too
        return super.toString();
    }

    // funkcija print pieņem int tipa vērtību tabulasPlatums un neatgriež nekādu
    // vērtību
    // public void print(int width) {
    // // Izmanto formatēto izvadi, lai dati konsolē izskatītos sakārtoti
    // (izlīdzināti pēc platuma)
    // System.out.printf("Auto tips: %-14s; Cena par stundu: %d; Tarifa tips: %-" +
    // width + "s; Pilna cena: %d\n",
    // autoType, hourRate, name, fullPrice);
    // }

    // funkcija fromCSV pieņem String tipa vērtību csvDati un atgriež Rate tipa
    // vērtību jaunsTarifs
    public static Rate fromCSV(String csvdata) throws Exception {
        // Sadala saņemto teksta rindu masīvā, izmantojot komatu kā atdalītāju
        String[] fields = csvdata.split(",");

        // Pārbauda, vai rindā ir pietiekami daudz datu lauku, lai izveidotu objektu
        if (fields.length < 9) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 9");
        }

        // Konvertē teksta vērtības uz atbilstošajiem datu tipiem
        int id = Integer.valueOf(fields[0]);
        int parkId = Integer.valueOf(fields[1]);
        AutoType autoType = AutoType.valueOf(fields[2]);
        RateType rateType = RateType.valueOf(fields[3]);
        float price = Float.valueOf(fields[4]);
        LocalTime startTime = LocalTime.parse(fields[5]);
        LocalTime endTime = LocalTime.parse(fields[6]);
        int multipleCount = Integer.valueOf(fields[7]);
        byte weekDays = Byte.valueOf(fields[8]);

        return new Rate(id, parkId, autoType, rateType, price, startTime, endTime, multipleCount, weekDays);
    }
}
