public class Park implements CSVEncodable {
    private int id;
    private String name;
    private String address;
    private String district;
    
    // funkcija Park pieņem int tipa vērtību id, String tipa vērtību name,
    // String tipa vērtību address un String tipa vērtību district un atgriež Park tipa objektu
    public Park(int id, String name, String address, String district) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.district = district;
    }

    // funkcija atgriež int tipa vērtību id
    public int id() {
        return id;
    }

    // funkcija atgriež String tipa vērtību name
    public String name() {
        return name;
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

    // funkcija toString atgriež String tipa vērtību
    public String toString() {
        return name + "; " + address + "; " + district;
    }

    // funkcija toCSV atgriež String tipa vērtību
    public String toCSV() {
        return id + "," + name + "," + address + "," + district + "\n";
    }

    // funkcija print neko neatgriež un neko nepienem
    public void print(int name_width, int address_width) {
        System.out.printf("Nosaukums: %-" + name_width + "s; Adrese: %-" +
                address_width + "s; Rajons: %-14s\n",
                name, address, district);
    }

    // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa
    // vērtību
    public static Park fromCSV(String csvdata) throws Exception {
        String[] fields = csvdata.split(",");
        if (fields.length < 4) {
            throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 4");
        }
        int id = Integer.valueOf(fields[0]);
        String name = fields[1];
        String address = fields[2];
        String district = fields[3];

        return new Park(id, name, address, district);
    }
}
