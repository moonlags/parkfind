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

    public int parkId() {
        return parkId;
    }

    public int id() {
        return id;
    }

    // funkcija toCSV atgriež String tipa vērtību
    public String toCSV() {
        return id + "," + parkId + "," + autoType.name() + "," + rateType.name() + "," + price + "," + startTime + ","
                + endTime + "," + multipleCount + "," + weekDays + "\n";
    }

    public String toString() {
        // TODO add complex toString, that check rateType and print accordingly
        // Include id too
        return super.toString();
    }

    // funkcija print neko neatgriež un neko nepienem
    // public void print(int width) {
    // System.out.printf("Auto tips: %-14s; Cena par stundu: %d; Tarifa tips: %-" +
    // width + "s; Pilna cena: %d\n",
    // autoType, hourRate, name, fullPrice);
    // }

    // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa
    // vērtību
    public static Rate fromCSV(String csvdata) throws Exception {
        String[] fields = csvdata.split(",");
        if (fields.length < 9) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 6");
        }
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
