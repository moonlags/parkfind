import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

enum RateType {
  Hour {
    public String toString() {
      return "Stundas";
    }
  },
  Day {
    public String toString() {
      return "Dienas posma";
    }
  },
  Month {
    public String toString() {
      return "Menēša";
    }
  },
}

public class Rate implements CSVEncodable, TablePrintable {
  private int id;
  private int parkId;
  private AutoType autoType;
  private RateType rateType;
  private float price;
  private LocalTime startTime;
  private LocalTime endTime;
  private int amount;
  private byte weekDays;
  private float freeHours;

  // funkcija Rate pieņem int tipa vērtību id, int tipa vērtību parkId, AutoType
  // tipa vērtību autoType, RateType tipa vērtību rateType, float tipa vērtību
  // price, LocalTime tipa vērtību startTime, LocalTime tipa vērtību endTime, int
  // tipa vērtību mutipleCount, byte tipa vērtību weekDays, float tipa vērtību
  // freeHours un neatgriež nekādu vērtību
  public Rate(int id, int parkId, AutoType autoType, RateType rateType, float price, LocalTime startTime,
      LocalTime endTime, int amount, byte weekDays, float freeHours) {
    this.id = id;
    this.parkId = parkId;
    this.autoType = autoType;
    this.rateType = rateType;
    this.price = price;
    this.startTime = startTime;
    this.endTime = endTime;
    this.amount = amount;
    this.weekDays = weekDays;
    this.freeHours = freeHours;
  }

  // funkcija price atgriež float tipa vērtību price
  public float price() {
    return price;
  }

  public byte weekDays() {
    return weekDays;
  }

  public RateType rateType() {
    return rateType;
  }

  public LocalTime startTime() {
    return startTime;
  }

  public LocalTime endTime() {
    return endTime;
  }

  public float freeHours() {
    return freeHours;
  }

  public int amount() {
    return amount;
  }

  public AutoType autoType() {
    return autoType;
  }

  public int getPayments(long hours, long days, long months) {
    switch (rateType) {
      case Hour:
        double billableHours = Math.max(0.0, hours - freeHours);
        if (billableHours <= 0)
          break;

        double rawHours = billableHours / amount; // rate.amount() is the billing unit (hours)
        return (int) Math.ceil(rawHours); // round up to next whole payment unit
      case Day:
        double rawDays = (double) days / amount; // rate.amount() is the billing unit (days)
        return (int) Math.ceil(rawDays); // round up to next whole payment unit
      case Month:
        double rawMonths = (double) months / amount; // rate.amount() is the billing unit
                                                     // (months)
        return (int) Math.ceil(rawMonths); // round up to next whole payment unit
    }
    return 9999; // unreachable
  }

  public double calculatePrice(LocalDateTime startTime, LocalDateTime endTime) {
    long months = ChronoUnit.MONTHS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
    long days = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
    long hours = Util.hoursBetweenDates(startTime, endTime);

    return price * getPayments(hours, days, months);
  }

  // funkcija enterNew pieņem Scanner tipa vērtību scanner, int tipa vērtību id,
  // int tipa vērtību parkId un atgriež Rate tipa vērtību rate
  public static Rate enterNew(Scanner scanner, int id, int parkId) throws Exception {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Izvēlēties jebkuru auto tipu",
            "Izvēlēties elektro auto tipu", "Atpakal"));
    int choice = Menu.printMenu(
        scanner, choices);

    AutoType autoType;
    switch (choice) {
      case 1:
        autoType = AutoType.Any;
        break;
      case 2:
        autoType = AutoType.Electro;
        break;
      default:
        throw new Exception("Tarifa izveide ir apturēta!");
    }

    choices = new ArrayList<>(
        Arrays.asList("Izveidot stundas tipa tarifu", "Izveidot dienas posma tipa tarifu",
            "Izveidot mēneša tipa tarifu", "Atpakal"));
    choice = Menu.printMenu(scanner, choices);

    RateType rateType;
    switch (choice) {
      case 1:
        rateType = RateType.Hour;
        break;
      case 2:
        rateType = RateType.Day;
        break;
      case 3:
        rateType = RateType.Month;
        break;
      default:
        throw new Exception("Tarifa izveide ir apturēta!");
    }

    System.out.print("Ievadi pilnu cenu: ");
    float price;
    try {
      price = Float.valueOf(scanner.nextLine());
    } catch (Exception e) {
      throw new Exception("Cenai ir jābut reālajam skaitlim!");
    }

    if (price < 0) {
      throw new Exception("Cena nevar but negatīva!");
    }

    float freeHours = 0;
    if (rateType == RateType.Hour) {
      System.out.print("Ievadi bezmaksas stundu daudzumu: ");
      try {
        freeHours = Float.valueOf(scanner.nextLine());
      } catch (Exception e) {
        throw new Exception("Bezmaksas stundas daudzums nav pareizi ievadīts!");
      }
    }

    System.out.print(
        "Ievadi laiku, kad tarifs saka darboties (piem. 05:34)\nVai nospied Enter, lai izslēgtu sākuma laika ierobiežojumus: ");
    LocalTime startTime = LocalTime.MIDNIGHT;
    try {
      String in = scanner.nextLine();
      if (!in.isEmpty()) {
        startTime = LocalTime.parse(in, DateTimeFormatter.ofPattern("H:mm"));
      }
    } catch (Exception e) {
      throw new Exception("Sakuma laiks nav pareizi ievadits!");
    }

    System.out.print(
        "Ievadi laiku, kad tarifs beidz darboties (piem. 18:02)\nVai nospied Enter, lai izslēgtu beigu laika ierobiežojumus: ");
    LocalTime endTime = startTime;
    try {
      String in = scanner.nextLine();
      if (!in.isEmpty()) {
        endTime = LocalTime.parse(in, DateTimeFormatter.ofPattern("H:mm"));
      }
    } catch (Exception e) {
      throw new Exception("Beigu laiks nav pareizi ievadīts!");
    }

    System.out.print("Ievadi stundu/dienu/menēšu daudzumu: ");
    int multipleCount = 1;
    try {
      multipleCount = Integer.valueOf(scanner.nextLine());
    } catch (Exception e) {
      throw new Exception("Daudzumam ir jābut naturālajam skaitlim");
    }

    if (multipleCount <= 0) {
      throw new Exception("Daudzumam ir jābut pozitīvam!");
    }

    System.out.print("Ievadi nedēļas dienas, kad tarifs ir aktīvs (piem. \'1,5,7\'), vai \'visas\': ");
    byte weekdays = 0;
    String wdStr = scanner.nextLine();
    if (wdStr.equals("visas")) {
      wdStr = "1,2,3,4,5,6,7";
    }

    for (String dayStr : wdStr.split(",")) {
      int day;
      try {
        day = Integer.valueOf(dayStr);
      } catch (Exception e) {
        throw new Exception("Nedēļas dienas nav ievadītas korekti!");
      }
      if (day <= 0 || day > 7) {
        throw new Exception("Diena " + day + " nav ievadīta korekti!");
      }
      weekdays = (byte) (weekdays | 1 << (day - 1));
    }

    return new Rate(id, parkId, autoType, rateType, price, startTime, endTime, multipleCount, weekdays, freeHours);
  }

  // funkcija parkId nepieņem nevienu vērtību un atgriež int tipa vērtību parkId
  public int parkId() {
    return parkId;
  }

  // funkcija id atgriež int tipa vērtību id
  public int id() {
    return id;
  }

  // funkcija toCSV nepieņem nevienu vērtību un atgriež String tipa vērtību toCSV
  public String toCSV() {
    return id + "," + parkId + "," + autoType.name() + "," + rateType.name() + "," + price + "," + startTime + ","
        + endTime + "," + amount + "," + weekDays + "," + freeHours + "\n";
  }

  // funkcija toTableRow pieņem List<Integer> tipa vērtību widths un atgriež
  // String tipa vērtību tableRow
  public String toTableRow(List<Integer> widths) {
    String formatString = widths.stream()
        .map(w -> "%-" + w + "s")
        .collect(Collectors.joining(" | ", "| ", " |"));

    String wkd = "";
    int i = 1;
    byte temp = weekDays;
    while (temp > 0) {
      if ((temp & 1) == 1) {
        wkd += i + ",";
      }
      temp = (byte) (temp >> 1);
      i++;
    }
    wkd = wkd.substring(0, wkd.length() - 1);

    return String.format(formatString, id, autoType, rateType, price, wkd, startTime, endTime, amount,
        freeHours);
  }

  // funkcija fromCSV pieņem String tipa vērtību csvData un atgriež Rate tipa
  // vērtību noCSV
  public static Rate fromCSV(String csvdata) throws Exception {
    // Sadala saņemto teksta rindu masīvā, izmantojot komatu kā atdalītāju
    String[] fields = csvdata.split(",");

    // Pārbauda, vai rindā ir pietiekami daudz datu lauku, lai izveidotu objektu
    if (fields.length < 10) {
      throw new Exception("Nepareizs csv datu skaits: saņēma " + fields.length + ", gaidīja 10");
    }

    // Konvertē teksta vērtības uz atbilstošajiem datu tipiem
    int id = Integer.valueOf(fields[0]);
    int parkId = Integer.valueOf(fields[1]);
    AutoType autoType = AutoType.valueOf(fields[2]);
    RateType rateType = RateType.valueOf(fields[3]);
    float price = Float.valueOf(fields[4]);
    LocalTime startTime = LocalTime.parse(fields[5]);
    LocalTime endTime = LocalTime.parse(fields[6]);
    int amount = Integer.valueOf(fields[7]);
    byte weekDays = Byte.valueOf(fields[8]);
    float freeHours = Float.valueOf(fields[9]);

    return new Rate(id, parkId, autoType, rateType, price, startTime, endTime, amount, weekDays, freeHours);
  }
}
