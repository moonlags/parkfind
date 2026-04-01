public class Rate {
    private int id;
    private int parkId;
    private AutoType autoType;
    private float hourRate;
    private String name;
    private float fullPrice;

    public Rate(int id, int parkId, AutoType autoType, float hourRate, String name, float fullPrice) {
        this.id = id;
        this.parkId = parkId;
        this.autoType = autoType;
        this.hourRate = hourRate;
        this.name = name;
        this.fullPrice = fullPrice;
    }

    // funkcija toCSV atgriež String tipa vērtību
    public String toCSV() {
        return id + "," + parkId + "," + autoType.ordinal() + "," + hourRate + "," + name + "," + fullPrice + "\n";
    }

    // funkcija print neko neatgriež un neko nepienem
    public void print(int width) {
        System.out.printf("Auto tips: %-14s; Cena par stundu: %d; Tarifa tips: %-" + width + "s; Pilna cena: %d\n",
                autoType, hourRate, name, fullPrice);
    }

    // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa
    // vērtību
    public static Rate fromCSV(String csvdata) throws Exception {
        String[] fields = csvdata.split(",");
        if (fields.length < 6) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 6");
        }
        int id = Integer.valueOf(fields[0]);
        int parkId = Integer.valueOf(fields[1]);
        AutoType autoType = fields[1];
        String address = fields[2];
        String district = fields[3];

        return new Park(id, name, address, district);
    }
}