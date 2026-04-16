import java.util.TreeSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
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

    List<Integer> max_column_widths = List.of(address_width, 25, 9);
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
    LocalDateTime rateStartTime = startTime;
    AtomicBoolean stop = new AtomicBoolean(false);
    Rate currentRate = chosen.rate();

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

    LocalDateTime now = LocalDateTime.now();

    for (Rate rate : chosen.usedRates()) {
      if (rate.worksInTime(now)) {
        currentRate = rate;
        break;
      }
    }

    boolean firstRate = true;
    double totalPrice = 0;
    try {
      while (!stop.get()) {
        LocalDateTime timeNow = LocalDateTime.now();

        clearConsole();

        boolean isCurrentRateValid = currentRate.worksInTime(timeNow);
        if (!isCurrentRateValid) {
          boolean foundNewRate = false;
          Rate oldRate = currentRate;

          firstRate = false;

          for (Rate rate : chosen.usedRates()) {
            Rate nextRate = rate;

            if (nextRate.worksInTime(timeNow)) {
              totalPrice += oldRate.calculatePrice(rateStartTime, timeNow, false);

              currentRate = nextRate;
              rateStartTime = timeNow;
              foundNewRate = true;
              break;
            }
          }

          if (!foundNewRate) {
            Color.error("Neizdevās atrast nākamo piemēroto tarifu!");
            stop.set(true);
            break;
          }
        }

        long hours = Util.hoursBetweenDates(rateStartTime, timeNow);
        if (hours <= currentRate.freeHours() && hours != 0) {
          System.out.println("Tagad tiek izmantotas " + currentRate.freeHours() + " bezmaksas stundas!");
        }

        double displayPrice = totalPrice
            + currentRate.calculatePrice(rateStartTime, timeNow, firstRate);
        System.out.println("Jūs jau stāvējāt autostāvvietā ar adresi " + chosen.park().address() + " - "
            + HumanReadable.formatInterval(startTime, timeNow)
            + " un samaksājāt " + String.format("%.2f", displayPrice) + " EUR!\nUzspiediet ENTER lai pabeigtu:");

        // Sleep ~1 second between updates, but wake sooner if interrupted
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          stop.set(true);
        }
      }

      // record final end time
      LocalDateTime finalEndTime = LocalDateTime.now();
      totalPrice += currentRate.calculatePrice(rateStartTime, finalEndTime, firstRate);

      clearConsole();

      System.out.println("Jūs stāvējāt " + chosen.park().address() + " autostāvvieta: ");
      System.out.println(HumanReadable.formatInterval(startTime, finalEndTime));
      System.out.println("Un paterējāt " + String.format("%.2f", totalPrice) + " EUR");

      if (!parkings.containsKey(curr.email())) {
        parkings.put(curr.email(), new ArrayList<>());
      }

      ArrayList<Parking> temp = parkings.get(curr.email());
      Parking parking = new Parking(newId, startTime, finalEndTime,
          totalPrice, curr.email(), chosen.park().id(), chosen.park().address());

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
          results = findBestParks();
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

  public ArrayList<SearchResult> findBestParks() throws Exception {
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
      if (p.district().equals(district)) {
        System.out.println(p.address());
        parksInSameDistrict.add(p);
      }
    }

    PriorityQueue<SearchResult> results = new PriorityQueue<>(Comparator.comparing(SearchResult::price));

    for (Park park : parksInSameDistrict) {
      if (!rates.containsKey(park.id()))
        continue;

      // Filter to rates compatible with this autoType
      ArrayList<Rate> compatibleRates = new ArrayList<>();
      for (Rate rate : rates.get(park.id())) {
        if (rate.autoType() == AutoType.Electro && curr.autoType() != AutoType.Electro)
          continue;
        compatibleRates.add(rate);
      }

      SearchResult res = findBestParksRecursive(compatibleRates, startTime, endTime, 1);
      if (res != null) {
        results.add(new SearchResult(park, res.usedRates().get(0), res.price(), res.usedRates()));
      }
    }

    ArrayList<SearchResult> top5 = new ArrayList<>(5);
    for (int i = 0; i < 5 && !results.isEmpty(); i++)
      top5.add(results.poll());

    return top5;
  }

  private SearchResult findBestParksRecursive(ArrayList<Rate> compatibleRates, LocalDateTime cursor,
      LocalDateTime endTime, int depth) {
    if (!cursor.isBefore(endTime)) {
      return new SearchResult(null, 0.0, new ArrayList<>());
    } else if (depth >= 5) {
      return null;
    }

    SearchResult bestResult = null;
    double bestPrice = Double.MAX_VALUE;

    for (Rate rate : compatibleRates) {
      // Check weekday is allowed for this date
      if (!Util.isDateAllowedByWeekdays(cursor.toLocalDate(), rate.weekDays()))
        continue;

      // Check time window: if start==end, rate has no time restriction
      if (!rate.startTime().equals(rate.endTime())) {
        boolean inWindow;
        if (!rate.startTime().isAfter(rate.endTime())) {
          // Normal window e.g. 08:00-20:00
          inWindow = !cursor.toLocalTime().isBefore(rate.startTime())
              && cursor.toLocalTime().isBefore(rate.endTime());
        } else {
          // Overnight window e.g. 20:00-08:00
          inWindow = !cursor.toLocalTime().isBefore(rate.startTime())
              || cursor.toLocalTime().isBefore(rate.endTime());
        }
        if (!inWindow)
          continue;
      }

      // Compute the end of this rate's window for the current segment
      LocalDateTime candidateEnd = Util.calculateWindowEnd(rate, cursor, endTime);
      double price = rate.calculatePrice(cursor, candidateEnd, depth == 1);

      SearchResult nextPart = findBestParksRecursive(compatibleRates, candidateEnd, endTime, depth++);
      if (nextPart == null)
        continue;

      double combinedPrice = price + nextPart.price();
      if (combinedPrice < bestPrice) {
        bestPrice = combinedPrice;

        ArrayList<Rate> allUsedRates = new ArrayList<>();
        allUsedRates.add(rate);
        allUsedRates.addAll(nextPart.usedRates());

        bestResult = new SearchResult(null, combinedPrice, allUsedRates);
      }
    }

    if (bestResult == null)
      return null;

    return bestResult;
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
        parking.setAddress(parks.get(parking.parkId()).address());

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
    System.out.println("Esi sveicināts Autostāvvietu meklēšanā!");

    loadUsers();
    loadParks();
    loadRates();
    loadParkings();

    while (true) {
      page = page.invoke();
    }
  }
}
