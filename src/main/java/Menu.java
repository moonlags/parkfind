import java.util.Scanner;

public class Menu {
  // funkcija printMenu pieņem Scanner tipa vērtību scanner, String[] tipa vērtību choices un
  // atgriež int tipa vērtību choice
  static int printMenu(Scanner scanner, String[] choices) {
    System.out.println();
    for (int i = 0; i < choices.length; i++) {
      System.out.printf("%d - %s;\n", i + 1, choices[i]);
    }
    while (true) {
      System.out.printf("Izvelies darbibu (%d-%d): ", 1, choices.length);
      try {

        int choice = Integer.valueOf(scanner.nextLine());

        if (choice >= 1 && choice <= choices.length) {
          System.out.println("--------------------------------------------");
          return choice;
        }
        System.out.println("Nav tadas darbibas!");
      } catch (NumberFormatException e) {
        System.out.println("Ievadi skaitli!");
      }
    }
  }
}
