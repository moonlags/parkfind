// TODO: hash passwords

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@FunctionalInterface
interface HandlerFn {
  // funkcija invoke atgriež HandlerFn tipa vērtību
  HandlerFn invoke();
}

public class UserInterface {
  private HandlerFn page;
  private User curr;

  private int chosenParkId;

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
      throw new Exception("Tarifi nav atrasti!");

    List<String> columnNames = List.of("Adrese", "Tarifa tips", "Cena");

    int address_width = 6;
    for (SearchResult res : options) {
      if (res.park().address().length() > address_width)
        address_width = res.park().address().length();
    }

    List<Integer> max_column_widths = List.of(address_width, 12, 9);
    Table.printTable(columnNames, max_column_widths, options);

    ArrayList<String> choices = new ArrayList<>();
    // TODO: garumzimes
    choices.add("Atpakal");
    for (SearchResult res : options) {
      choices.add(res.park().address());
    }

    System.out.println("Izvēlies autostavvietu!");
    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        // TODO: garumzimes
        throw new Exception("Taimera startēšana apturēta!");
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
            + " un esat samaksājāt " + String.format("%.2f", price) + " EUR!\nUzspiediet ENTER lai pabeigtu:");

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
          totalPrice, curr.email(), chosen.park().id(),
          chosen.rate().id());
      parking.setPark(parks.get(parking.parkId()));

      temp.add(parking);
      newId++;
      parkings.put(curr.email(), temp);

      try {
        parkingFile.appendOne(parking);
      } catch (Exception e) {
        Color.warn("Neizdevas pievienot vesturi failam: " + e);
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
        Arrays.asList("Atrast autostavvietu", "Apskatīt vēsturi", "Izdzēst visu vēsturi", "Izmainīt iestatījumus",
            "Samainit paroli",
            "Dzest kontu", "Atpakal",
            "Iziet"));

    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();

        ArrayList<SearchResult> results;
        try {
          results = Park.findBestParkings(scanner, parks, rates, curr.autoType());
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

        startTimer(scanner, chosen);
        break;
      case 2:
        clearConsole();
        if (parkings.size() == 0) {
          Color.error("Nav vēstures");
          break;
        }

        List<String> columnNames = List.of("Adrese", "Sākuma laiks", "Beigu laiks", "Cena");

        int address_width = 6;
        for (Parking parking : parkings.get(curr.email())) {
          if (parking.park().address().length() > address_width)
            address_width = parking.park().address().length();
        }

        List<Integer> max_column_widths = List.of(address_width, 16, 16, 9);

        Table.printTable(columnNames, max_column_widths, parkings.get(curr.email()));
        break;
      case 3:
        clearConsole();
        parkings.put(curr.email(), new ArrayList<>());
        Color.success("Vēsture is izdzēstā!");

        saveParkings();
        break;
      case 4:
        clearConsole();
        System.out.print("Vai jums ir elektromašīna? (Jā/Nē): ");
        if (scanner.nextLine().equals("Jā")) {
          curr.setAutoType(AutoType.Electro);
        }
        Color.success("Iestatījumi ir atjaunināti!");

        saveUsers();

        break;
      case 5:
        clearConsole();
        try {
          curr.changePassword(scanner, users);
          clearConsole();
          Color.success("Paroles maina ir veiksmiga!");
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
        Color.success("Jusu konts ir dzests!");

        saveUsers();
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
        Arrays.asList("Apskatit lietotajus", "Apskatit autostavvietas", "Samainit paroli", "Atpakal", "Iziet"));
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
          Color.success("Paroles maina ir veiksmiga!");
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
        Arrays.asList("Izvadit lietotajus", "Dzest lietotajus", "Atpakal", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        if (users.size() == 0) {
          Color.error("Nav lietotaju");
          break;
        }

        List<String> columnNames = List.of("E-pasts", "Loma", "Talruna numurs", "Automasinas tips");

        int email_width = 7;
        for (String email : users.keySet()) {
          if (email.length() > email_width)
            email_width = email.length();
        }

        List<Integer> max_column_widths = List.of(email_width, 14, 14, 16);

        Table.printTable(columnNames, max_column_widths, List.copyOf(users.values()));
        break;
      case 2:
        clearConsole();
        System.out.print("Ievadiet lietotaja e-pastu: ");

        String email = scanner.nextLine();
        if (!users.containsKey(email)) {
          Color.error("E-pasts nav atrasts!");
          break;
        } else if (curr.email().equals(email)) {
          Color.error("Jus nevarat izdzest sevi!");
          break;
        }

        users.remove(email);
        Color.success("Lietotajs ir izdzests!");

        saveUsers();
        break;
      case 3:
        clearConsole();
        return this::adminPage;
      case 4:
        clearConsole();
        System.out.println("Visu labu");
        System.exit(0);
    }

    return this::usersActionsPage;
  }

  // funkcija parksActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn parksActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Pievienot autostavvietu", "Apskatit autostavvietu sarakstu", "Rediget autostavvietu", "Atpakal",
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
        break;
      case 3:
        clearConsole();
        if (parks.size() == 0) {
          Color.error("Nav autostavvietu");
          break;
        }

        columnNames = List.of("ID", "Nosaukums", "Adrese", "Rajons");

        id_width = 2;
        name_width = 9;
        address_width = 6;
        district_width = 6;
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

        temp = new ArrayList<>(parks.values());
        temp.sort(Comparator.comparing(Park::id));

        max_column_widths = List.of(id_width, name_width, address_width, district_width);

        Table.printTable(columnNames, max_column_widths, temp);

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
        System.out.println("Visu labu");
        System.exit(0);
    }

    return this::parksActionsPage;
  }

  // funkcija singleParkActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn singleParkActionsPage() {
    // TODO: hide parks from users as disabled
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Nomainit nosaukumu", "Nomainit adresi", "Nomainit rajonu", "Apskatit tarifus",
            "Dzest autostāvvietu", "Atpakal",
            "Iziet"));
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
        Color.success("Nosaukums ir nomainits");

        saveParks();
        break;
      case 2:
        clearConsole();
        System.out.print("Ievadiet jaunu adresi: ");
        String newAddress = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setAddress(newAddress);
        parks.put(temp.id(), temp);
        Color.success("Adrese ir izmainita");

        saveParks();
        break;
      case 3:
        clearConsole();
        System.out.print("Ievadiet jaunu rajonu: ");
        String newDistrict = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setDistrict(newDistrict);
        parks.put(temp.id(), temp);
        Color.success("Rajons ir nomainits");

        saveParks();
        break;
      case 4:
        clearConsole();
        return this::rateActionsPage;
      case 5:
        clearConsole();
        System.out.print("Vai tiešām dzest (Ja/Ne)?: ");
        String y = scanner.nextLine();

        if (!y.equals("Ja"))
          break;

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
        System.out.println("Visu labu");
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
          Color.success("Jauns tarifs ir veiksmigi izveidots!");
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
        try {
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
          Color.error("Ievadi pareizo tarifa ID!");
        }
        break;
      case 3:
        clearConsole();
        if (rates.get(chosenParkId) == null || rates.get(chosenParkId).size() == 0) {
          Color.error("Nav tarifu");
          break;
        }

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
        break;
      case 4:
        clearConsole();
        return this::singleParkActionsPage;
      case 5:
        clearConsole();
        System.out.println("Visu labu");
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
          Color.warn("Neizdevas pievienot lietotaju failam: " + e);
        }

        return this::userPage;
      case 2:
        clearConsole();
        try {
          curr = User.login(scanner, users);
          clearConsole();
          System.out.println("Jūs esat veiksmīgi atgriezusies sistema!");

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

  // funkcija loadUsers neko nepieņem un neko neatgriež
  private void loadUsers() {
    try {
      ArrayList<User> userArray = userFile.loadAll();
      for (User user : userArray) {
        users.put(user.email(), user);
      }
    } catch (Exception e) {
      Color.warn("Neizdevas ieladet lietotajus: " + e);
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
      Color.warn("Neizdevas ieladet autostavvietu: " + e);
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
      Color.warn("Neizdevas ieladet tarifu: " + e);
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

        parking.setPark(parks.get(parking.parkId()));
        temp.add(parking);
        parkings.put(parking.email(), temp);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt stavēšanas: " + e);
    }
  }

  private void saveParkings() {
    try {
      for (ArrayList<Parking> parkingsForUser : parkings.values()) {
        parkingFile.writeAll(parkingsForUser);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās stavēšanas pievienot failos: " + e);
    }
  }

  // funkcija saveUsers neko nepieņem un neko neatgriež
  private void saveUsers() {
    try {
      userFile.writeAll(users.values());
    } catch (Exception e) {
      Color.warn("Neizdevās lietotājus pievienot failos: " + e);
    }
  }

  // funkcija saveParks neko nepieņem un neko neatgriež
  private void saveParks() {
    try {
      parkFile.writeAll(parks.values());
    } catch (Exception e) {
      Color.warn("Neizdevās autostāvvietas pievienot failos: " + e);
    }
  }

  // funkcija saveRates neko nepieņem un neko neatgriež
  private void saveRates() {
    try {
      for (ArrayList<Rate> ratesForParks : rates.values()) {
        rateFile.writeAll(ratesForParks);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās tarifus pievienot failos: " + e);
    }
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
