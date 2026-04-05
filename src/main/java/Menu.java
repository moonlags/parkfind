import java.util.Scanner;
import java.util.List;

public class Menu {
  // funkcija printMenu pieņem Scanner tipa vērtību scanner, String[] tipa vērtību
  // choices un
  // atgriež int tipa vērtību choice
  static int printMenu(Scanner scanner, List<String> choices) {
    System.out.println();

    // Izdrukā visas masīvā esošās izvēlnes iespējas, pievienojot tām numuru
    for (int i = 0; i < choices.size(); i++) {
      System.out.printf("%d - %s;\n", i + 1, choices.get(i));
    }

    // Mūžīgais cikls nodrošina to, ka programma nebeidzas, kamēr nav saņemta derīga
    // ievade
    while (true) {
      System.out.printf("Izvēlies darbību (%d-%d): ", 1, choices.size());
      try {

        int choice = Integer.valueOf(scanner.nextLine());

        if (choice >= 1 && choice <= choices.size()) {
          UserInterface.clearConsole();
          return choice;
        }
        Color.error("Nav tādas darbības!");
      } catch (NumberFormatException e) {
        Color.error("Ievadi skaitli!");
      }
    }
  }
}
