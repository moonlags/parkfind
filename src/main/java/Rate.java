public class Rate implements CSVEncodable {
    // TODO: under question, better add starttime, endtime, 7 bit integer for
    // weekdays and hoiday boolean
    private int id;
    private int parkId;
    private AutoType autoType;
    private float hourRate;
    private String name;
    private float fullPrice;

    // funkcija Rate pieņem int, int, AutoType, float, String, float tipa vērtības id, parkId, autoType, hourRate, name, fullPrice un neatgriež nekādu vērtību
    public Rate(int id, int parkId, AutoType autoType, float hourRate, String name, float fullPrice) {
        this.id = id;
        this.parkId = parkId;
        this.autoType = autoType;
        this.hourRate = hourRate;
        this.name = name;
        this.fullPrice = fullPrice;
    }

    // funkcija parkId nepieņem nevienu vērtību un atgriež int tipa vērtību parkId
    public int parkId() {
        return parkId;
    }

    // funkcija toCSV nepieņem nevienu vērtību un atgriež String tipa vērtību csvRinda
    public String toCSV() {
        // Apvieno visus klases laukus vienā tekstā, atdalot tos ar komatiem priekš CSV formāta
        return id + "," + parkId + "," + autoType.name() + "," + hourRate + "," + name + "," + fullPrice + "\n";
    }

    // funkcija print pieņem int tipa vērtību tabulasPlatums un neatgriež nekādu vērtību
    public void print(int width) {
        // Izmanto formatēto izvadi, lai dati konsolē izskatītos sakārtoti (izlīdzināti pēc platuma)
        System.out.printf("Auto tips: %-14s; Cena par stundu: %d; Tarifa tips: %-" + width + "s; Pilna cena: %d\n",
                autoType, hourRate, name, fullPrice);
    }

    // funkcija fromCSV pieņem String tipa vērtību csvDati un atgriež Rate tipa vērtību jaunsTarifs
    public static Rate fromCSV(String csvdata) throws Exception {
        // Sadala saņemto teksta rindu masīvā, izmantojot komatu kā atdalītāju
        String[] fields = csvdata.split(",");

        // Pārbauda, vai rindā ir pietiekami daudz datu lauku, lai izveidotu objektu
        if (fields.length < 6) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 6");
        }

        // Konvertē teksta vērtības uz atbilstošajiem datu tipiem
        int id = Integer.valueOf(fields[0]);
        int parkId = Integer.valueOf(fields[1]);
        AutoType autoType = AutoType.valueOf(fields[2]);
        float hourRate = Float.valueOf(fields[3]);
        String name = fields[4];
        float fullPrice = Float.valueOf(fields[5]);

        return new Rate(id, parkId, autoType, hourRate, name, fullPrice);
    }
}
