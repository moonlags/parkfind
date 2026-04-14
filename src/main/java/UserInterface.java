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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.Arrays;

@FunctionalInterface
interface HandlerFn {
  // funkcija invoke atgriež HandlerFn tipa vērtību
  HandlerFn invoke();
}

public class UserInterface {
  private HandlerFn page;
  private User curr;

  private int chosenParkId;
  private String chosenUserEmail;

  private int newId;

  private Scanner scanner;

  private HashMap<String, User> users;
  private FileHandler<User> userFile;

  private HashMap<Integer, Park> parks;
  private FileHandler<Park> parkFile;

  private HashMap<Integer, ArrayList<Rate>> rates;
  private FileHandler<Rate> rateFile;

  private HashMap<String, ArrayList<Parking>> parkings;
  private FileHandler<Parking> parkingFile;

  public UserInterface() {
    users = new HashMap<>();
    parks = new HashMap<>();
    rates = new HashMap<>();
    parkings = new HashMap<>();

    scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    userFile = new FileHandler<>("data/users.csv", User::fromCSV);
    parkFile = new FileHandler<>("data/parks.csv", Park::fromCSV);
    rateFile = new FileHandler<>("data/rates.csv", Rate::fromCSV);
    parkingFile = new FileHandler<>("data/parkings.csv", Parking::fromCSV);

    page = this::loginPage;
    newId = 1;
  }

  private SearchResult chooseSearchResult(ArrayList<SearchResult> options) throws Exception {
    if (options.isEmpty())
      throw new Exception("Pieejamie tarifi nav atrasti!");

    System.out.println("Pieejamās stāvvietas rajonā: " + options.get(1).park().district());

    List<String> columnNames = List.of("Adrese", "Tarifa tips", "Cena");

    int address_width = 6;
    for (SearchResult res : options) {
      if (res.park().address().length() > address_width)
        address_width = res.park().address().length();
    }

    List<Integer> max_column_widths = List.of(address_width, 12, 9);
    Table.printTable(columnNames, max_column_widths, options);
    System.out.println("Tika atrāsti " + options.size() + " varianti!");

    ArrayList<String> choices = new ArrayList<>();
    choices.add("Atpakaļ");
    for (SearchResult res : options) {
      choices.add(res.park().address());
    }

    System.out.println("Izvēlies autostavvietu!");
    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        throw new Exception("Taimera startēšana tika apturēta!");
      default:
        return options.get(choice - 2);
    }
  }

  private void startTimer(Scanner scanner, SearchResult chosen) {
    ExecutorService ex = Executors.newSingleThreadExecutor();
    LocalDateTime startTime = LocalDateTime.now();
    AtomicBoolean stop = new AtomicBoolean(false);
    final LocalDateTime[] finalEndTimeHolder = new LocalDateTime[1];

    // Task that blocks waiting for ENTER
    Future<?> reader = ex.submit(() -> {
      try {
        // Block until user presses ENTER or scanner is closed
        scanner.nextLine();
        stop.set(true);
      } catch (NoSuchElementException | IllegalStateException e) {
        // input closed; signal stop
        stop.set(true);
      }
    });

    try {
      while (!stop.get()) {
        LocalDateTime timeNow = LocalDateTime.now();
        double price = chosen.rate().calculatePrice(startTime, timeNow);

        clearConsole();

        // check rate availability based on current time
        if (!chosen.rate().startTime().equals(chosen.rate().endTime())) {
          if (timeNow.toLocalTime().isAfter(chosen.rate().endTime())
              || timeNow.toLocalTime().isBefore(chosen.rate().startTime())
              || !Util.isDateAllowedByWeekdays(timeNow.toLocalDate(), chosen.rate().weekDays())) {
            Color.error("Izvēlētais tarifs tagad nestrādā!");
            finalEndTimeHolder[0] = timeNow;
            stop.set(true);
            break;
          }
        }

        long hours = ChronoUnit.HOURS.between(startTime, timeNow) + 1;
        if (hours <= chosen.rate().freeHours()) {
          System.out.println("Tagad tiek izmantotas " + chosen.rate().freeHours() + " bezmaksas stundas!");
        }

        System.out.println("Jūs jau stāvējāt autostāvvietā ar adresi " + chosen.park().address() + " - "
            + HumanReadable.formatInterval(startTime, timeNow)
            + " un samaksājāt " + String.format("%.2f", price) + " EUR!\nUzspiediet ENTER lai pabeigtu:");

        // Sleep ~1 second between updates, but wake sooner if interrupted
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          stop.set(true);
        }
      }

      // record final end time
      LocalDateTime finalEndTime = finalEndTimeHolder[0] != null ? finalEndTimeHolder[0] : LocalDateTime.now();

      clearConsole();

      double totalPrice = chosen.rate().calculatePrice(startTime, finalEndTime);

      System.out.println("Jūs stāvējāt " + chosen.park().address() + " autostāvvieta: ");
      System.out.println(HumanReadable.formatInterval(startTime, finalEndTime));
      System.out.println("Un paterējāt " + String.format("%.2f", totalPrice) + " EUR");

      if (!parkings.containsKey(curr.email())) {
        parkings.put(curr.email(), new ArrayList<>());
      }

      ArrayList<Parking> temp = parkings.get(curr.email());
      Parking parking = new Parking(newId, startTime, finalEndTime,
          totalPrice, curr.email(),
          chosen.rate().id(), chosen.park().id(), chosen.park().address());

      temp.add(parking);
      newId++;
      parkings.put(curr.email(), temp);

      try {
        parkingFile.appendOne(parking);
      } catch (Exception e) {
        Color.warn("Neizdevās pievienot vēsturi failam: " + e);
      }
    } finally {
      // ensure reader thread is stopped
      stop.set(true);
      reader.cancel(true);
      ex.shutdownNow();
    }
  }

  // funkcija userPage atgriež HandlerFn tipa vērtību
  private HandlerFn userPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Atrāst autostāvvietu", "Apskatīt vēsturi", "Izdzēst visu vēsturi", "Izmainīt iestatījumus",
            "Nomainīt paroli", "Dzēst kontu", "Atpakaļ", "Iziet"));

    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();

        ArrayList<SearchResult> results;
        try {
          results = findBestParkings();
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        SearchResult chosen;
        try {
          chosen = chooseSearchResult(results);
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        System.out.println("Stāvēšana ir aktivizēta!");
        startTimer(scanner, chosen);
        break;
      case 2:
        clearConsole();
        if (!parkings.containsKey(curr.email())) {
          Color.error("Jums vēl nav vēstures!");
          break;
        }

        printParkingsTable(curr.email());
        break;
      case 3:
        clearConsole();
        parkings.put(curr.email(), new ArrayList<>());
        Color.success("Vēsture is izdzēstā!");

        saveParkings();
        break;
      case 4:
        clearConsole();
        System.out.print("Vai Jums ir elektromašīna? (j/n): ");
        String in = scanner.nextLine();
        if (in.equalsIgnoreCase("j")) {
          curr.setAutoType(AutoType.Electro);
          Color.success("Iestatījumi ir atjaunināti!");

          saveUsers();
        } else if (in.equalsIgnoreCase("n")) {
          curr.setAutoType(AutoType.Any);
          Color.success("Iestatījumi ir atjaunināti!");

          saveUsers();
        } else {
          Color.error("Nepareiza ievade!");
        }
        break;
      case 5:
        clearConsole();
        try {
          curr.changePassword(scanner, users);
          clearConsole();
          Color.success("Paroles maiņa ir veiksmīga!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        saveUsers();
        break;
      case 6:
        clearConsole();

        System.out.print("Ievadiet paroli: ");
        String pwd = scanner.nextLine();
        if (!curr.checkPassword(pwd)) {
          Color.error("Parole nav pareiza!");
          break;
        }

        users.remove(curr.email());
        parkings.remove(curr.email());
        Color.success("Jūsu konts ir dzests!");

        saveUsers();
        saveParkings();
        return this::loginPage;
      case 7:
        clearConsole();
        return this::loginPage;
      case 8:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::userPage;
  }

  // funkcija adminPage atgriež HandlerFn tipa vērtību
  private HandlerFn adminPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Apskatīt lietotājus", "Apskatīt autostāvvietas", "Nomainīt paroli", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        return this::usersActionsPage;
      case 2:
        clearConsole();
        return this::parksActionsPage;
      case 3:
        clearConsole();
        try {
          curr.changePassword(scanner, users);
          clearConsole();
          Color.success("Paroles maiņa ir veiksmīga!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        saveUsers();
        break;
      case 4:
        clearConsole();
        return this::loginPage;
      case 5:
        clearConsole();
        System.out.println("Visu labu");
        System.exit(0);
    }

    return this::adminPage;
  }

  // funkcija usersActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn usersActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Izvadīt lietotājus", "Apskatīt lietotāju", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        if (users.size() == 0) {
          Color.error("Nav lietotāju!");
          break;
        }

        printUsersTable();
        break;
      case 2:
        clearConsole();
        if (users.size() == 0) {
          Color.error("Nav lietotāju!");
          break;
        }
        printUsersTable();

        System.out.print("Ievadiet lietotāja e-pastu: ");
        try {
          String email = scanner.nextLine();
          if (!users.containsKey(email)) {
            Color.error("Lietotājs nav atrasts!");
            break;
          }

          chosenUserEmail = email;
          return this::singleUserActionsPage;
        } catch (Exception e) {
          Color.error("Autostāvvietas id nav ievadīts pareizi!");
          break;
        }
      case 3:
        clearConsole();
        return this::adminPage;
      case 4:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::usersActionsPage;
  }

  private HandlerFn singleUserActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Izvadīt vēsturi", "Dzēst lietotāju", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        if (!parkings.containsKey(chosenUserEmail)) {
          Color.error("Lietotājam vēl nav vēstures");
          break;
        }

        printParkingsTable(chosenUserEmail);
        break;
      case 2:
        clearConsole();

        if (users.get(chosenUserEmail).role() == UserRole.Admin) {
          Color.error("Jūs nevarat izdzēst administratoru!");
          break;
        }

        System.out.print("Vai tiešām dzēst (j/n)?: ");
        String y = scanner.nextLine();

        if (!y.equalsIgnoreCase("j")) {
          Color.error("Dzēšana ir apturēta!");
          break;
        }

        users.remove(chosenUserEmail);
        Color.success("Lietotājs ir izdzēsts!");

        saveUsers();
        return this::usersActionsPage;
      case 3:
        clearConsole();
        return this::usersActionsPage;
      case 4:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::singleUserActionsPage;
  }

  // funkcija parksActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn parksActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Pievienot autostāvvietu", "Apskatīt autostāvvietu sarakstu", "Rediģēt autostāvvietu", "Atpakaļ",
            "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();

        Park park;
        try {
          park = Park.enterNew(scanner, newId);
          clearConsole();
          Color.success("Jaunā autostāvvietа ir veiksmīgi izveidota!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        newId++;
        parks.put(park.id(), park);

        try {
          parkFile.appendOne(park);
        } catch (Exception e) {
          Color.warn("Neizdevās pievienot autostāvvietu failam: " + e);
        }

        break;
      case 2:
        clearConsole();
        if (parks.size() == 0) {
          Color.error("Nav autostāvvietu");
          break;
        }

        printParksTable();
        break;
      case 3:
        clearConsole();
        if (parks.size() == 0) {
          Color.error("Nav autostāvvietu");
          break;
        }
        printParksTable();

        System.out.print("Ievadiet autostāvvietas id: ");
        try {
          int id = Integer.valueOf(scanner.nextLine());
          if (!parks.containsKey(id)) {
            Color.error("Autostāvvietа nav atrasta!");
            break;
          }

          chosenParkId = id;
          return this::singleParkActionsPage;
        } catch (Exception e) {
          Color.error("Autostāvvietas id nav ievadīts pareizi!");
          break;
        }
      case 4:
        clearConsole();
        return this::adminPage;
      case 5:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::parksActionsPage;
  }

  // funkcija singleParkActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn singleParkActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Nomainīt nosaukumu", "Nomainīt adresi", "Nomainīt rajonu", "Apskatīt tarifus",
            "Dzēst autostāvvietu", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    Park temp = null;
    switch (choice) {
      case 1:
        clearConsole();
        System.out.print("Ievadiet jaunu nosaukumu: ");
        String newName = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setName(newName);
        parks.put(temp.id(), temp);
        Color.success("Nosaukums ir nomainīts!");

        saveParks();
        break;
      case 2:
        clearConsole();
        System.out.print("Ievadiet jaunu adresi: ");
        String newAddress = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setAddress(newAddress);
        parks.put(temp.id(), temp);
        Color.success("Adrese ir izmainīta!");

        saveParks();
        break;
      case 3:
        clearConsole();
        System.out.print("Ievadiet jaunu rajonu: ");
        String newDistrict = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setDistrict(newDistrict);
        parks.put(temp.id(), temp);
        Color.success("Rajons ir nomainīts!");

        saveParks();
        break;
      case 4:
        clearConsole();
        return this::rateActionsPage;
      case 5:
        clearConsole();
        System.out.print("Vai tiešām dzēst (j/n)?: ");
        String y = scanner.nextLine();

        if (!y.equalsIgnoreCase("j")) {
          Color.error("Dzēšana ir apturēta!");
          break;
        }

        parks.remove(chosenParkId);
        chosenParkId = -1;

        Color.success("Autostāvvietа dzēsta");

        saveParks();
        return this::parksActionsPage;
      case 6:
        clearConsole();
        chosenParkId = -1;
        return this::parksActionsPage;
      case 7:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::singleParkActionsPage;
  }

  // funkcija rateActionsPage atgriež HandlerFn tipa vērtību
  public HandlerFn rateActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Pievienot tarifu", "Dzēst tarifu", "Apskatīt tarifu sarakstu", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    ArrayList<Rate> temp;
    switch (choice) {
      case 1:
        clearConsole();

        Rate rate;
        try {
          rate = Rate.enterNew(scanner, newId, chosenParkId);
          clearConsole();
          Color.success("Jauns tarifs ir veiksmīgi izveidots!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        newId++;
        if (!rates.containsKey(chosenParkId)) {
          rates.put(chosenParkId, new ArrayList<>());
        }
        temp = rates.get(chosenParkId);
        temp.add(rate);
        rates.put(chosenParkId, temp);

        try {
          rateFile.appendOne(rate);
        } catch (Exception e) {
          Color.warn("Neizdevās pievienot tarifu failam: " + e);
        }

        break;
      case 2:
        clearConsole();
        if (!rates.containsKey(chosenParkId)) {
          Color.error("Nav tarifu");
          break;
        }
        printRatesTable();

        try {
          System.out.println("Ievadiet tarifa ID: ");
          int id = Integer.valueOf(scanner.nextLine());

          temp = rates.get(chosenParkId);
          int i = 0;
          boolean found = false;
          for (Rate el : temp) {
            if (el.id() == id) {
              found = true;
              temp.remove(i);
              break;
            }
            i++;
          }

          if (!found) {
            Color.error("Tarifs nav atrasts!");
            break;
          }

          rates.put(chosenParkId, temp);
          saveRates();

          Color.success("Tarifs ir dzēsts");
        } catch (Exception e) {
          Color.error("Tarifa ID nav ievadīts pareizi!");
        }
        break;
      case 3:
        clearConsole();
        if (!rates.containsKey(chosenParkId)) {
          Color.error("Nav tarifu");
          break;
        }

        printRatesTable();
        break;
      case 4:
        clearConsole();
        return this::singleParkActionsPage;
      case 5:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::rateActionsPage;
  }

  // funkcija loginPage atgriež HandlerFn tipa vērtību
  private HandlerFn loginPage() {
    curr = null;

    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Reģistrēties", "Ienākt sava konta", "Iziet"));
    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        try {
          curr = User.register(scanner, users);
          clearConsole();
          Color.success("Reģistrācija ir veiksmīga!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        try {
          userFile.appendOne(curr);
        } catch (Exception e) {
          Color.warn("Neizdevās pievienot lietotāju failam: " + e);
        }

        return this::userPage;
      case 2:
        clearConsole();
        try {
          curr = User.login(scanner, users);
          clearConsole();
          Color.success("Jūs esat veiksmīgi atgriezusies sistēmā!");

          saveUsers();

          if (curr.role() == UserRole.Admin) {
            return this::adminPage;
          } else {
            return this::userPage;
          }
        } catch (Exception e) {
          Color.error(e.getMessage());
        }
        break;
      case 3:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::loginPage;
  }

  public ArrayList<SearchResult> findBestParkings() throws Exception {
    System.out
        .print(
            "Ievadi laiku un datumu, kad plāno atstāt automašīnu autostāvvietā (piem. 09:49 08.04.2026)\nVai nospied Enter, lai ievadītu pašreizejo datumu: ");
    LocalDateTime startTime = LocalDateTime.now();
    try {
      String in = scanner.nextLine();
      if (!in.isEmpty()) {
        startTime = LocalDateTime.parse(in, DateTimeFormatter.ofPattern("H:mm dd.MM.yyyy"));
      }
    } catch (Exception e) {
      throw new Exception("Sākuma laiks nav pareizi ievadīts!");
    }

    if (startTime.isBefore(LocalDateTime.now().minusMinutes(1)))
      throw new Exception("Sākuma laiks nevar būt pagātnē!");

    System.out
        .print("Ievadi paredzemo beigu laiku un datumu, kad izbrauksi no autostāvvietas (piem. 10:03 09.04.2026): ");
    LocalDateTime endTime = LocalDateTime.now();
    try {
      endTime = LocalDateTime.parse(scanner.nextLine(), DateTimeFormatter.ofPattern("H:mm dd.MM.yyyy"));
    } catch (Exception e) {
      throw new Exception("Beigu laiks nav pareizi ievadīts!");
    }

    if (!endTime.isAfter(startTime))
      throw new Exception("Beigu laiks nevar būt mazāks vai vienāds ar sākuma laiku!");

    HashSet<String> districts = new HashSet<>();
    for (Park park : parks.values()) {
      districts.add(park.district());
    }
    ArrayList<String> choices = new ArrayList<>();
    choices.add("Atpakaļ");
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
      if (p.district().equals(district))
        parksInSameDistrict.add(p);
    }

    long months = ChronoUnit.MONTHS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
    long days = ChronoUnit.DAYS.between(startTime.toLocalDate(), endTime.toLocalDate()) + 1;
    long hours = Util.hoursBetweenDates(startTime, endTime);

    TreeSet<SearchResult> results = new TreeSet<>(Comparator.comparing(SearchResult::price));
    for (Park park : parksInSameDistrict) {
      if (!rates.containsKey(park.id()))
        continue;

      for (Rate rate : rates.get(park.id())) {

        if (rate.autoType() == AutoType.Electro && curr.autoType() != AutoType.Electro)
          continue;

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

        if (!rate.startTime().equals(rate.endTime())) {
          boolean startOutside = startTime.toLocalTime().isBefore(rate.startTime());
          boolean endOutside = endTime.toLocalTime().isAfter(rate.endTime());
          if (startOutside || endOutside)
            continue;
          else if (startTime.toLocalTime().isAfter(endTime.toLocalTime())
              && !rate.startTime().isAfter(rate.endTime())) { // night rate
            continue;
          }
        }

        double price = rate.price() * rate.getPayments(hours, days, months);
        results.add(new SearchResult(park, rate, price));
      }
    }

    ArrayList<SearchResult> top5 = new ArrayList<>(5);
    Iterator<SearchResult> it = results.iterator();
    for (int i = 0; i < 5 && it.hasNext(); i++)
      top5.add(it.next());

    return top5;
  }

  // funkcija loadUsers neko nepieņem un neko neatgriež
  private void loadUsers() {
    try {
      ArrayList<User> userArray = userFile.loadAll();
      for (User user : userArray) {
        users.put(user.email(), user);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt lietotājus: " + e);
    }
  }

  // funkcija loadParks neko nepieņem un neko neatgriež
  private void loadParks() {
    try {
      ArrayList<Park> parkArray = parkFile.loadAll();
      for (Park park : parkArray) {
        parks.put(park.id(), park);
        if (park.id() >= newId)
          newId = park.id() + 1;
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt autostāvvietas: " + e);
    }
  }

  // funkcija loadRates neko nepieņem un neko neatgriež
  private void loadRates() {
    try {
      ArrayList<Rate> rateArray = rateFile.loadAll();
      for (Rate rate : rateArray) {
        if (!rates.containsKey(rate.parkId())) {
          rates.put(rate.parkId(), new ArrayList<>());
        }
        if (rate.id() >= newId)
          newId = rate.id() + 1;
        ArrayList<Rate> temp = rates.get(rate.parkId());
        temp.add(rate);
        rates.put(rate.parkId(), temp);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt tarifus: " + e);
    }
  }

  private void loadParkings() {
    try {
      ArrayList<Parking> parkingArray = parkingFile.loadAll();
      for (Parking parking : parkingArray) {
        if (!parkings.containsKey(parking.email())) {
          parkings.put(parking.email(), new ArrayList<>());
        }

        if (parking.id() >= newId)
          newId = parking.id() + 1;
        ArrayList<Parking> temp = parkings.get(parking.email());
        temp.add(parking);
        parkings.put(parking.email(), temp);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt stavēšanas: " + e);
    }
  }

  private void saveParkings() {
    try {
      List<Parking> all = parkings.values().stream().flatMap(List::stream).collect(Collectors.toList());
      parkingFile.writeAll(all);
    } catch (Exception e) {
      Color.warn("Neizdevās stavēšanas pievienot failā: " + e);
    }
  }

  // funkcija saveUsers neko nepieņem un neko neatgriež
  private void saveUsers() {
    try {
      userFile.writeAll(users.values());
    } catch (Exception e) {
      Color.warn("Neizdevās lietotājus pievienot failā: " + e);
    }
  }

  // funkcija saveParks neko nepieņem un neko neatgriež
  private void saveParks() {
    try {
      parkFile.writeAll(parks.values());
    } catch (Exception e) {
      Color.warn("Neizdevās autostāvvietas pievienot failā: " + e);
    }
  }

  // funkcija saveRates neko nepieņem un neko neatgriež
  private void saveRates() {
    try {
      List<Rate> all = rates.values().stream().flatMap(List::stream).collect(Collectors.toList());
      rateFile.writeAll(all);
    } catch (Exception e) {
      Color.warn("Neizdevās tarifus pievienot failā: " + e);
    }
  }

  private void printParksTable() {
    List<String> columnNames = List.of("ID", "Nosaukums", "Adrese", "Rajons");

    int id_width = 2;
    int name_width = 9;
    int address_width = 6;
    int district_width = 6;
    for (Park p : parks.values()) {
      if (String.valueOf(p.id()).length() > id_width)
        id_width = String.valueOf(p.id()).length();
      if (p.name().length() > name_width)
        name_width = p.name().length();
      if (p.address().length() > address_width)
        address_width = p.address().length();
      if (p.district().length() > district_width)
        district_width = p.district().length();
    }

    ArrayList<Park> temp = new ArrayList<>(parks.values());
    temp.sort(Comparator.comparing(Park::id));

    List<Integer> max_column_widths = List.of(id_width, name_width, address_width, district_width);

    Table.printTable(columnNames, max_column_widths, temp);
  }

  private void printUsersTable() {
    List<String> columnNames = List.of("E-pasts", "Loma", "Tālruņa numurs", "Automašīnas tips");

    int email_width = 7;
    for (String email : users.keySet()) {
      if (email.length() > email_width)
        email_width = email.length();
    }

    List<Integer> max_column_widths = List.of(email_width, 14, 14, 16);

    Table.printTable(columnNames, max_column_widths, List.copyOf(users.values()));
  }

  private void printParkingsTable(String email) {
    List<String> columnNames = List.of("Adrese", "Sākuma laiks", "Beigu laiks", "Cena");

    int address_width = 6;
    for (Parking parking : parkings.get(email)) {
      if (parking.address().length() > address_width)
        address_width = parking.address().length();
    }

    List<Integer> max_column_widths = List.of(address_width, 16, 16, 9);

    Table.printTable(columnNames, max_column_widths, parkings.get(email));
  }

  private void printRatesTable() {
    List<String> columnNames = List.of("ID", "Auto tips", "Tarifa tips", "Pilna cena", "Nedēļas dienas",
        "Sākuma laiks", "Beigu laiks", "Daudzums", "Bezmaksas stundas");

    int id_width = 2;
    int price_width = 10;
    for (Rate p : rates.get(chosenParkId)) {
      if (String.valueOf(p.id()).length() > id_width)
        id_width = String.valueOf(p.id()).length();
      if (String.valueOf(p.price()).length() > price_width)
        price_width = String.valueOf(p.price()).length();
    }

    List<Integer> max_column_widths = List.of(id_width, 9, 14, price_width, 14, 12, 11, 8, 17);

    Table.printTable(columnNames, max_column_widths, List.copyOf(rates.get(chosenParkId)));
  }

  // funkcija clearConsole nepieņem parametrus un neatgriež nevienu vērtību
  public static void clearConsole() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  // funkcija run neko nepieņem un neko neatgriež
  public void run() {
    clearConsole();
    System.out.println("Esi sveicinats Autostāvvietu meklēšanā!");

    loadUsers();
    loadParks();
    loadRates();
    loadParkings();

    while (true) {
      page = page.invoke();
    }
  }
}
