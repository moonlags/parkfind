import java.time.LocalTime;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

// TODO: add toString methods
enum RateType {
  PerHour,
  DayPart,
  DayWhole,
  MonthWhole,
  MonthPart,
  DaysMultiple,
  HoursMultiple
}

public class Rate implements CSVEncodable, TablePrintable {
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

  public float price() {
    return price;
  }

  // TODO: garumzimes
  public static Rate enterNew(Scanner scanner, int id, int parkId) throws Exception {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Izveleties jebkuru auto tipu",
            "Izveleties elektro auto tipu", "Atpakal"));
    int choice = Menu.printMenu(
        scanner, choices);

    AutoType autoType;
    switch (choice) {
      case 1:
        autoType = AutoType.Any;
      case 2:
        autoType = AutoType.Electro;
      case 3:
        throw new Exception("Tarifa izveide ir aptureta!");
      default:
        autoType = AutoType.Any;
    }

    choices = new ArrayList<>(
        Arrays.asList("Izveidot stundas tipa tarifu", "Izveidot dienas posma tipa tarifu",
            "Izveidot pilnas dienas tipa tarifu", "Izveidot pilna mēneša tipa tarifu",
            "Izveidot mēneša perioda tipa tarifu", "Izveidot vairāku dienu tipa tarifu",
            "Izveidot vairaaku stundu tipa tarifu", "Atpakal"));
    choice = Menu.printMenu(
        scanner, choices);

    RateType rateType;
    switch (choice) {
      case 1:
        rateType = RateType.PerHour;
      case 2:
        rateType = RateType.DayPart;
      case 3:
        rateType = RateType.DayWhole;
      case 4:
        rateType = RateType.MonthWhole;
      case 5:
        rateType = RateType.MonthPart;
      case 6:
        rateType = RateType.DaysMultiple;
      case 7:
        rateType = RateType.HoursMultiple;
      case 8:
        throw new Exception("Tarifa izveide ir aptureta!");
      default:
        rateType = RateType.PerHour;
    }

    System.out.print("Ievadi pilnu cenu: ");
    float price;
    try {
      price = Float.valueOf(scanner.nextLine());
    } catch (Exception e) {
      throw new Exception("Cenai ir jabut realajam skaitlim!");
    }

    if (price < 0) {
      throw new Exception("Cena nevar but negativa!");
    }

    LocalTime startTime = LocalTime.MIDNIGHT;
    LocalTime endTime = startTime;
    if (rateType != RateType.DayWhole) {
      System.out.print("Ievadi laiku, kad tarifs saka darboties (hh:mm): ");
      try {
        startTime = LocalTime.parse(scanner.nextLine());
      } catch (Exception e) {
        throw new Exception("Sakuma laiks nav pareizi ievadits!");
      }

      System.out.print("Ievadi laiku, kad tarifs beidz darboties (hh:mm): ");
      try {
        endTime = LocalTime.parse(scanner.nextLine());
      } catch (Exception e) {
        throw new Exception("Beigu laiks nav pareizi ievadits!");
      }

      if (endTime.isBefore(startTime)) {
        throw new Exception("Beigu laiks nevar but mazaks par sakuma laiku!");
      }
    }

    int multipleCount = 1;
    if (rateType == RateType.DaysMultiple || rateType == RateType.HoursMultiple) {
      System.out.print("Ievadi daudzumu: ");
      try {
        multipleCount = Integer.valueOf(scanner.nextLine());
      } catch (Exception e) {
        throw new Exception("Daudzumam ir jabut naturalajam skaitlim");
      }

      if (multipleCount <= 0) {
        throw new Exception("Daudzumam ir jabut pozitivam!");
      }
    }

    System.out.print("Ievadi nedelas dienas, kad tarifs ir aktivs (piem. \'1,5,7\'), vai \'visas\': ");
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
        throw new Exception("Nedelas dienas nav ievaditas korekti!");
      }
      if (day <= 0 || day > 7) {
        throw new Exception("Diena " + day + " nav ievadita korekti!");
      }
      weekdays = (byte) (weekdays | 1 << (day - 1));
    }

    return new Rate(id, parkId, autoType, rateType, price, startTime, endTime, multipleCount, weekdays);
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

    return String.format(formatString, id, autoType, rateType, price, wkd, startTime, endTime, multipleCount);
  }

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
