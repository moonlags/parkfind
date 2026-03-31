// TODO: visu izvadi latviesu valoda

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class UserInterface {
  private HandlerFn page;
  private User curr;
  private HashMap<String, User> users;
  private Scanner scanner;
  private FileHandler<User> userFile;

  public UserInterface() {
    users = new HashMap<>();
    scanner = new Scanner(System.in);
    userFile = new FileHandler<>("data/users.csv", User::fromCSV);
    page = this::loginPage;
  }

  // funkcija userPage atgriež HandlerFn tipa vērtību
  private HandlerFn userPage() {
    int choice =
        Menu.printMenu(
            scanner,
            new String[] {
              "Atrast autostavvietu", "Samainit paroli", "Dzest kontu", "Atpakal", "Iziet"
            });

    switch (choice) {
      case 1:
        // TODO: implement
        break;
      case 2:
        try {
          curr.changePassword(scanner, users);
          System.out.println("Paroles maina ir veiksmiga!");
        } catch (Exception e) {
          System.out.println(e.getMessage());
          break;
        }

        saveUsers();
        break;
      case 3:
        users.remove(curr.email());
        System.out.println("Jusu konts ir dzests!");

        saveUsers();
        return this::loginPage;
      case 4:
        return this::loginPage;
      case 5:
        System.out.println("Davaj poka!");
        System.exit(0);
    }

    return this::userPage;
  }

  // funkcija adminPage atgriež HandlerFn tipa vērtību
  private HandlerFn adminPage() {
    int choice =
        Menu.printMenu(
            scanner,
            new String[] {
              "Apskatit lietotajus",
              "Apskatit autostavvietas",
              "Samainit paroli",
              "Atpakal",
              "Iziet"
            });

    switch (choice) {
      case 1:
        return this::usersActionsPage;
      case 2:
        return this::parksActionsPage;
      case 3:
        try {
          curr.changePassword(scanner, users);
          System.out.println("Paroles maina ir veiksmiga!");
        } catch (Exception e) {
          System.out.println(e.getMessage());
          break;
        }

        saveUsers();
        break;
      case 4:
        return this::loginPage;
      case 5:
        System.out.println("Davaj poka!");
        System.exit(0);
    }

    return this::adminPage;
  }

  // funkcija usersActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn usersActionsPage() {
    int choice =
        Menu.printMenu(
            scanner, new String[] {"Izvadit lietotajus", "Dzest lietotaju", "Atpakal", "Iziet"});

    switch (choice) {
      case 1:
        int max_width = 0;
        for (String email : users.keySet()) {
          if (email.length() > max_width) max_width = email.length();
        }

        for (User user : users.values()) {
          user.print(max_width);
        }
        break;
      case 2:
        System.out.print("Ievadiet lietotaja e-pastu: ");

        String email = scanner.nextLine();
        if (!users.containsKey(email)) {
          System.out.println("E-pasts nav atrasts!");
          break;
        } else if (curr.email().equals(email)) {
          System.out.println("Jus nevarat izdzest sevi!");
          break;
        }

        users.remove(email);
        System.out.println("Lietotajs ir izdzests!");

        saveUsers();
        break;
      case 3:
        return this::adminPage;
      case 4:
        System.out.println("Davaj poka!");
        System.exit(0);
    }

    return this::usersActionsPage;
  }

  // funkcija parksActionsPage atgriež HandlerFn tipa vērtību
  private HandlerFn parksActionsPage() {
    // TODO: implement
    return this::parksActionsPage;
  }

  // funkcija loginPage atgriež HandlerFn tipa vērtību
  private HandlerFn loginPage() {
    curr = null;
    int choice =
        Menu.printMenu(scanner, new String[] {"Registreties", "Ienakt sava konta", "Iziet"});

    switch (choice) {
      case 1:
        try {
          curr = User.register(scanner, users);
          System.out.println("Registracija ir veiksmiga!");
        } catch (Exception e) {
          System.out.println(e.getMessage());
          break;
        }

        try {
          userFile.appendOne(curr);
        } catch (Exception e) {
          System.err.println("Failed to append user to file: " + e);
        }

        return this::userPage;
      case 2:
        try {
          curr = User.login(scanner, users);
          System.out.println("Darou obratno!");

          saveUsers();

          if (curr.isAdmin()) {
            return this::adminPage;
          } else {
            return this::userPage;
          }
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
        break;
      case 3:
        System.out.println("Davaj poka!");
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
      System.err.println("Failed to load users: " + e);
    }
  }

  // funkcija saveUsers neko nepieņem un neko neatgriež
  private void saveUsers() {
    try {
      userFile.writeAll(users.values());
    } catch (Exception e) {
      System.err.println("Failed to write users to file: " + e);
    }
  }

  // funkcija run neko nepieņem un neko neatgriež
  public void run() {
    System.out.println("Esi sveicinats Park Find'aa!");

    loadUsers();

    while (true) {
      page = page.invoke();
    }
  }
}
