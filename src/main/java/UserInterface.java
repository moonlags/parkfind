// TODO: hash passwords

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

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

  public UserInterface() {
    users = new HashMap<>();
    parks = new HashMap<>();
    rates = new HashMap<>();

    scanner = new Scanner(System.in);

    userFile = new FileHandler<>("data/users.csv", User::fromCSV);
    parkFile = new FileHandler<>("data/parks.csv", Park::fromCSV);
    rateFile = new FileHandler<>("data/rates.csv", Rate::fromCSV);

    page = this::loginPage;
    newId = 1;
  }

  // funkcija userPage atgriež HandlerFn tipa vērtību
  private HandlerFn userPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Atrast autostāvvietu", "Samainīt paroli", "Dzēst kontu", "Atpakaļ", "Iziet"));

    int choice = Menu.printMenu(scanner, choices);

    switch (choice) {
      case 1:
        // TODO: implement
        break;
      case 2:
        clearConsole();
        try {
          curr.changePassword(scanner, users);
          clearConsole();
          Color.success("Paroles maiņa ir veiksmiga!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        saveUsers();
        break;
      case 3:
        clearConsole();

        System.out.print("Ievadiet paroli: ");
        String pwd = scanner.nextLine();
        if (!curr.checkPassword(pwd)) {
          Color.error("Parole nav pareiza!");
          break;
        }

        users.remove(curr.email());
        Color.success("Jūsu konts ir dzēsts!");

        saveUsers();
        return this::loginPage;
      case 4:
        clearConsole();
        return this::loginPage;
      case 5:
        clearConsole();
        System.out.println("Visu labu!");
        System.exit(0);
    }

    return this::userPage;
  }

  // funkcija adminPage atgriež HandlerFn tipa vērtību
  private HandlerFn adminPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Apskatīt lietotājus", "Apskatīt autostāvvietas", "Samainīt paroli", "Atpakaļ", "Iziet"));
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
          Color.success("Paroles maiņa ir veiksmiga!");
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
        Arrays.asList("Izvadīt lietotājus", "Dzēst lietotājus", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        int max_width = 0;
        for (String email : users.keySet()) {
          if (email.length() > max_width)
            max_width = email.length();
        }

        for (User user : users.values()) {
          user.print(max_width);
        }
        break;
      case 2:
        clearConsole();
        System.out.print("Ievadiet lietotāja e-pastu: ");

        String email = scanner.nextLine();
        if (!users.containsKey(email)) {
          Color.error("E-pasts nav atrasts!");
          break;
        } else if (curr.email().equals(email)) {
          Color.error("Jus nevarat izdzēst sevi!");
          break;
        }

        users.remove(email);
        Color.success("Lietotājs ir izdzēsts!");

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
        Arrays.asList("Pievienot autostāvvietu", "Apskatīt autostāvvietu sarakstu", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:// TODO: garumzimes
        clearConsole();

        Park park;
        try {
          park = Park.enterNew(scanner, newId);
          clearConsole();
          Color.success("Jauna autostavvieta ir veiksmigi izveidota!");
        } catch (Exception e) {
          Color.error(e.getMessage());
          break;
        }

        newId++;
        parks.put(park.id(), park);

        try {
          parkFile.appendOne(park);
        } catch (Exception e) {
          Color.warn("Neizdevās pievienot autostavvietu failam: " + e);
        }

        break;
      case 2:
        clearConsole();
        return this::parkListActionsPage;
      case 3:
        clearConsole();
        return this::adminPage;
      case 4:
        clearConsole();
        System.out.println("Visu labu");
        System.exit(0);
    }

    return this::parksActionsPage;
  }

  private HandlerFn parkListActionsPage() {
    ArrayList<String> choices = new ArrayList<>();
    ArrayList<Integer> parkIds = new ArrayList<>();

    choices.add("Atpakaļ");
    choices.add("Iziet");

    for (Park park : parks.values()) {
      choices.add(park.toString());
      parkIds.add(park.id());
    }

    int choice = Menu.printMenu(
        scanner, choices);

    switch (choice) {
      case 1:
        clearConsole();
        return this::parksActionsPage;
      case 2:
        clearConsole();
        System.out.println("Visu labu");
        System.exit(0);
      default:
        clearConsole();
        System.out.println("Jūs izvēlējaties " + (choice - 2) + ". autostāvvietu!");
        chosenParkId = parkIds.get(choice - 2);
        return this::singleParkActionsPage;
    }
  }

  private HandlerFn singleParkActionsPage() {
    // TODO: hide parks from users as disabled
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Izmainīt nosaukumu", "Izmainīt adresi", "Izmainīt rajonu", "Apskatīt tarifus",
            "Dzēst autostāvvietu", "Atpakaļ",
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
        Color.success("Nosaukums ir izmainīts");

        saveParks();
        break;
      case 2:
        clearConsole();
        System.out.print("Ievadiet jaunu adresi: ");
        String newAddress = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setAddress(newAddress);
        parks.put(temp.id(), temp);
        Color.success("Adrese ir izmainīta");

        saveParks();
        break;
      case 3:
        clearConsole();
        System.out.print("Ievadiet jaunu rajonu: ");
        String newDistrict = scanner.nextLine();
        temp = parks.get(chosenParkId);

        temp.setDistrict(newDistrict);
        parks.put(temp.id(), temp);
        Color.success("Rajons ir izmainīts");

        saveParks();
        break;
      case 4:
        clearConsole();
        return this::rateActionsPage;
      case 5:
        clearConsole();
        System.out.print("Vai tiešam dzēst (Jā/Nē)?: ");
        String y = scanner.nextLine();

        if (!y.equals("Jā"))
          break;

        parks.remove(chosenParkId);
        chosenParkId = -1;

        Color.success("Autostāvvieta dzēsta");

        saveParks();
        return this::parksActionsPage;
      case 6:
        clearConsole();
        chosenParkId = -1;
        return this::parkListActionsPage;
      case 7:
        clearConsole();
        System.out.println("Visu labu");
        System.exit(0);
    }

    return this::singleParkActionsPage;
  }

  public HandlerFn rateActionsPage() {
    ArrayList<String> choices = new ArrayList<>(
        Arrays.asList("Pievienot tarifu", "Dzēst tarifu", "Apskatīt tarifu sarakstu", "Atpakaļ", "Iziet"));
    int choice = Menu.printMenu(
        scanner, choices);

    ArrayList<Rate> temp;
    switch (choice) {
      // TODO: garumzimes
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
      case 3:
        clearConsole();
        for (Rate el : rates.get(chosenParkId)) {
          System.out.println(el);
        }
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
        Arrays.asList("Reģistrēties", "Ienākt savā kontā", "Iziet"));
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
          System.out.println("Jūs esat veiksmīgi atgriezušies sistēmā!");

          saveUsers();

          if (curr.role() == UserRole.Admin) {
            return this::adminPage;
          } else {
            return this::userPage;
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());
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
      Color.warn("Neizdevās ielādēt lietotājus: " + e);
    }
  }

  // funkcija loadParks neko nepieņem un neko neatgriež
  private void loadParks() {
    try {
      ArrayList<Park> parkArray = parkFile.loadAll();
      for (Park park : parkArray) {
        parks.put(park.id(), park);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt autostāvvietu: " + e);
    }
  }

  // funkcija loadRates neko nepieņem un neko neatgriež
  // TODO
  private void loadRates() {
    try {
      ArrayList<Rate> rateArray = rateFile.loadAll();
      for (Rate rate : rateArray) {
        if (!rates.containsKey(rate.parkId())) {
          rates.put(rate.parkId(), new ArrayList<>());
        }
        rates.get(rate.parkId()).add(rate);
      }
    } catch (Exception e) {
      Color.warn("Neizdevās ielādēt tarifu: " + e);
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

  private void saveParks() {
    try {
      parkFile.writeAll(parks.values());
    } catch (Exception e) {
      Color.warn("Neizdevās autostavvietas pievienot failos: " + e); // TODO: garumzimes
    }
  }

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
    System.out.println("Esi sveicināts Autostāvvietu meklēšanā!");

    loadUsers();
    loadParks();
    loadRates();

    while (true) {
      page = page.invoke();
    }
  }
}
