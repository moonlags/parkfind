import java.util.Scanner;
import java.util.List;
import java.util.stream.Collectors;

public class Park implements CSVEncodable, TablePrintable {
    private int id;
    private String name;
    private String address;
    private String district;

    // funkcija Park pieņem int tipa vērtību id, String tipa vērtību name,
    // String tipa vērtību address un String tipa vērtību district un atgriež Park
    // tipa objektu
    public Park(int id, String name, String address, String district) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.district = district;
    }

    // funkcija enterNew pieņem Scanner tipa vērtību scanner un int tipa vērtību id
    // un atgriež Park tipa vērtību park
    public static Park enterNew(Scanner scanner, int id) throws Exception {
        System.out.println("Ievadiet autostāvvietas datus vai \'iziet\'!");
        System.out.print("Ievadi nosaukumu: ");
        String name = scanner.nextLine();
        if (name.equals("iziet"))
            throw new Exception("Autostāvvietas izveide ir apturēta!");
        else if (name.contains(","))
            throw new Exception("Neizmantojiet komatus!");

        System.out.print("Ievadi adresi: ");
        String address = scanner.nextLine();
        if (address.equals("iziet"))
            throw new Exception("Autostāvvietas izveide ir apturēta!");
        else if (address.contains(","))
            throw new Exception("Neizmantojiet komatus!");

        System.out.print("Ievadi rajonu: ");
        String district = scanner.nextLine();
        if (district.equals("iziet"))
            throw new Exception("Autostāvvietas izveide ir apturēta!");
        else if (district.contains(","))
            throw new Exception("Neizmantojiet komatus!");

        Park park = new Park(id, name, address, district);

        return park;
    }

    // funkcija atgriež int tipa vērtību id
    public int id() {
        return id;
    }

    // funkcija atgriež String tipa vērtību name
    public String name() {
        return name;
    }

    // funkcija atgriež String tipa vērtību address
    public String address() {
        return address;
    }

    // funkcija atgriež String tipa vērtību district
    public String district() {
        return district;
    }

    // funkcija setName pieņem String tipa vērtību name
    public void setName(String name) {
        this.name = name;
    }

    // funkcija setAddress pieņem String tipa vērtību address
    public void setAddress(String address) {
        this.address = address;
    }

    // funkcija setDistrict pieņem String tipa vērtību district
    public void setDistrict(String district) {
        this.district = district;
    }

    // funkcija toCSV atgriež String tipa vērtību
    public String toCSV() {
        return id + "," + name + "," + address + "," + district + "\n";
    }

    public String toTableRow(List<Integer> widths) {
        String formatString = widths.stream()
                .map(w -> "%-" + w + "s")
                .collect(Collectors.joining(" | ", "| ", " |"));

        return String.format(formatString, id, name, address, district);
    }

    // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež Park tipa
    // vērtību park
    public static Park fromCSV(String csvdata) throws Exception {
        String[] fields = csvdata.split(",");
        if (fields.length < 4) {
            throw new Exception("Nepareizs csv datu skaits: saņēma " + fields.length + ", gaidīja 4");
        }
        int id = Integer.valueOf(fields[0]);
        String name = fields[1];
        String address = fields[2];
        String district = fields[3];

        return new Park(id, name, address, district);
    }
}
