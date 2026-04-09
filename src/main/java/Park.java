import java.util.Scanner;
import java.util.TreeSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        System.out.println("Ievadiet autostavvietas datus vai \'iziet\'!");
        System.out.print("Ievadi nosaukumu: ");
        String name = scanner.nextLine();
        if (name.equals("iziet"))
            throw new Exception("Autostavvietas izveide ir aptureta!");

        System.out.print("Ievadi adresi: ");
        String address = scanner.nextLine();
        if (address.equals("iziet"))
            throw new Exception("Autostavvietas izveide ir aptureta!");

        System.out.print("Ievadi rajonu: ");
        String district = scanner.nextLine();
        if (district.equals("iziet"))
            throw new Exception("Autostavvietas izveide ir aptureta!");

        Park park = new Park(id, name, address, district);

        return park;
    }

    // TODO: garumzimes check
    public static ArrayList<SearchResult> findBestParkings(Scanner scanner, HashMap<Integer, Park> parks,
            HashMap<Integer, ArrayList<Rate>> rates, AutoType autoType) throws Exception {
        System.out
                .print("Ievadi laiku un datumu, kad plāno atstāt automašinu autostāvvietā (piem. 09:49 08.04.2026) vai nospied Enter: ");
        LocalDateTime startTime = LocalDateTime.now();
        try {
            String in = scanner.nextLine();
            if (!in.isEmpty()) {
                startTime = LocalDateTime.parse(in, DateTimeFormatter.ofPattern("H:mm dd.MM.yyyy"));
            }
        } catch (Exception e) {
            throw new Exception("Sakuma laiks nav pareizi ievadits!");
        }

        if (startTime.isBefore(LocalDateTime.now()))
            throw new Exception("Sākuma laiks nevar būt pagatnē!");

        System.out
                .print("Ievadi paredzemo beigu laiku un datumu, kad izbraukt no autostāvvietas (piem. 10:03 09.04.2026): ");
        LocalDateTime endTime = LocalDateTime.now();
        try {
            endTime = LocalDateTime.parse(scanner.nextLine(), DateTimeFormatter.ofPattern("H:mm dd.MM.yyyy"));
        } catch (Exception e) {
            throw new Exception("Beigu laiks nav pareizi ievadits!");
        }

        if (!endTime.isAfter(startTime))
            throw new Exception("Beigu laiks nevar būt mazāks vai vienāds ar sākuma laiku!");

        HashSet<String> districts = new HashSet<>();
        for (Park park : parks.values()) {
            districts.add(park.district);
        }
        ArrayList<String> choices = new ArrayList<>();
        // TODO: garumzimes
        choices.add("Atpakal");
        choices.addAll(districts);

        System.out.println("Izvēlies rajonu!");
        int choice = Menu.printMenu(scanner, choices);

        String district;
        switch (choice) {
            case 1:
                throw new Exception("Autostāvvietas meklēšana apturēta!");
            default:
                district = choices.get(choice - 1);
                break;
        }

        ArrayList<Park> parksInSameDistrict = new ArrayList<>();
        for (Park p : parks.values()) {
            if (p.district.equals(district))
                parksInSameDistrict.add(p);
        }

        long months = ChronoUnit.MONTHS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
        long days = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
        long hours = ChronoUnit.HOURS.between(startTime, endTime) + 1;

        TreeSet<SearchResult> results = new TreeSet<>(Comparator.comparing(SearchResult::price));
        for (Park park : parksInSameDistrict) {
            if (!rates.containsKey(park.id))
                continue;

            for (Rate rate : rates.get(park.id)) {
                int payments = 0;

                if (rate.autoType() != autoType)
                    continue;

                switch (rate.rateType()) {
                    case Hour:
                        double billableHours = Math.max(0.0, hours - rate.freeHours());
                        if (billableHours <= 0)
                            break;

                        double rawHours = billableHours / rate.amount(); // rate.amount() is the billing unit (hours)
                        payments = (int) Math.ceil(rawHours); // round up to next whole payment unit
                        break;
                    case Day:
                        double rawDays = (double) days / rate.amount(); // rate.amount() is the billing unit (days)
                        payments = (int) Math.ceil(rawDays); // round up to next whole payment unit
                        break;
                    case Month:
                        double rawMonths = (double) months / rate.amount(); // rate.amount() is the billing unit
                                                                            // (months)
                        payments = (int) Math.ceil(rawMonths); // round up to next whole payment unit
                        break;
                }

                byte rateWeekdays = rate.weekDays(); // e.g., 00000101b means Monday+Wednesday
                LocalDate cur = startTime.toLocalDate();
                LocalDate end = endTime.toLocalDate();
                // iterate each date covered by the booking; stop if any date not allowed
                boolean allowedWeekdays = true;
                while (!cur.isAfter(end)) {
                    if (!Util.isDateAllowedByWeekdays(cur, rateWeekdays)) {
                        allowedWeekdays = false;
                        break;
                    }
                    cur = cur.plusDays(1);
                }

                if (!allowedWeekdays)
                    continue;

                if (!rate.startTime().equals(rate.endTime()) && days > 1) {
                    boolean startOutside = startTime.toLocalTime().isBefore(rate.startTime());
                    boolean endOutside = endTime.toLocalTime().isAfter(rate.endTime());
                    if (startOutside || endOutside)
                        continue;

                    if (startTime.toLocalTime().isAfter(endTime.toLocalTime())
                            && !rate.startTime().isAfter(rate.endTime())) { // night rate
                        continue;
                    }
                }

                double price = rate.price() * payments;
                results.add(new SearchResult(park, rate, price));
            }
        }

        ArrayList<SearchResult> top5 = new ArrayList<>(5);
        Iterator<SearchResult> it = results.iterator();
        for (int i = 0; i < 5 && it.hasNext(); i++)
            top5.add(it.next());

        return top5;
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

    // funkcija toString atgriež String tipa vērtību
    public String toString() {
        return name + "; " + address + "; " + district;
    }

    // funkcija toCSV atgriež String tipa vērtību
    public String toCSV() {
        return id + "," + name + "," + address + "," + district + "\n";
    }

    // funkcija print neko nepieņem un neko neatgriež
    public void print(int name_width, int address_width) {
        System.out.printf("Nosaukums: %-" + name_width + "s; Adrese: %-" +
                address_width + "s; Rajons: %-14s\n",
                name, address, district);
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
            throw new Exception("Invalid csv fields: got " + fields.length + " expected 4");
        }
        int id = Integer.valueOf(fields[0]);
        String name = fields[1];
        String address = fields[2];
        String district = fields[3];

        return new Park(id, name, address, district);
    }
}
