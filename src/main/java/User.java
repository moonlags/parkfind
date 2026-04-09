import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

enum UserRole {
  User {
    public String toString() {
      return "Lietotājs";
    }
  },
  Admin {
    public String toString() {
      return "Administrators";
    }
  };
}

public class User implements CSVEncodable, TablePrintable {
  private String email;
  private String password;
  private UserRole role;
  private String phoneNumber;
  private AutoType autoType;

  // funkcija User pieņem String tipa vērtību email, String tipa vērtību password,
  // UserRole tipa vērtību role,
  // String tipa vērtību phoneNumber, AutoType tipa vērtību autoType un atgriež
  // User tipa objektu
  private User(String email, String password, UserRole role, String phoneNumber, AutoType autoType) {
    this.email = email;
    this.password = password;
    this.role = role;
    this.phoneNumber = phoneNumber;
    this.autoType = autoType;
  }

  // funkcija role atgriež UserRole tipa vērtību role
  public UserRole role() {
    return role;
  }

  public void setAutoType(AutoType autoType) {
    this.autoType = autoType;
  }

  public AutoType autoType() {
    return autoType;
  }

  // funkcija email atgriež String tipa vērtību email
  public String email() {
    return email;
  }

  // funkcija phoneNumber atgriež String tipa vērtību phoneNumber
  public String phoneNumber() {
    return phoneNumber;
  }

  // funkcija login pieņem Scanner tipa vērtību scanner, HashMap<String, User>
  // tipa vērtību users un
  // atgriež User tipa vērtību user
  public static User login(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.print("Ievadi e-pastu: ");
    String email = scanner.nextLine();

    System.out.print("Ievadi paroli: ");
    String pwd = scanner.nextLine();

    User user = users.get(email);
    if (user == null || !user.password.equals(pwd)) {
      throw new Exception("Nepareizs e-pasts vai parole!");
    }

    return user;
  }

  // funkcija register pieņem Scanner tipa vērtību scanner, HashMap<String, User>
  // tipa vērtību users un atgriež User tipa vērtību user
  public static User register(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.println("Reģistrācija (ieraksti \"iziet\" jebkurā brīdī, lai atceltu)");

    String email = readLineOrExit(scanner, "Ievadi e-pastu: ");
    while (!validateEmail(email) || users.containsKey(email)) {
      if (!validateEmail(email)) {
        Color.error("Nepareizs e-pasta formāts!");
      } else {
        Color.error("E-pasts ir jau izmantots!");
      }
      email = readLineOrExit(scanner, "Ievadi e-pastu: ");
    }

    String phoneNumber = readLineOrExit(scanner, "Ievadi tālruņa numuru (piem. +37121234567): ");
    while (phoneNumber.length() != 12 || phoneNumber.charAt(0) != '+' || phoneNumberExists(users, phoneNumber)) {
      if (phoneNumber.length() != 12 || phoneNumber.charAt(0) != '+') {
        Color.error("Nepareizs tālruņa numura formāts!");
      } else {
        Color.error("Tālruņa numurs ir jau izmantots!");
      }
      phoneNumber = readLineOrExit(scanner, "Ievadi tālruņa numuru (piem. +37121234567): ");
    }

    String pwd = readLineOrExit(scanner, "Izdomā paroli (8 simboli, mazie burti, lielie burti): ");
    while (!validatePassword(pwd) || pwd.contains(",")) {
      if (pwd.contains(",")) {
        Color.error("Neizmanojiet komatus!");
      } else {
        Color.error("Parolei jābūt vismaz 8 simbolu garai, saturēt mazos burtus, lielos burtus un ciparus!");
      }
      pwd = readLineOrExit(scanner, "Izdomā paroli: ");
    }

    String pwd2 = readLineOrExit(scanner, "Ievadi paroli vēlreiz: ");
    while (!pwd.equals(pwd2)) {
      Color.error("Paroles nesakrīt!");
      pwd2 = readLineOrExit(scanner, "Ievadi paroli vēlreiz: ");
    }

    User user = new User(email, pwd, UserRole.User, phoneNumber, AutoType.Any);
    users.put(email, user);
    Color.success("Reģistrācija veiksmīga!");
    return user;
  }

  // funkcija readLineOrExit pieņem Scanner tipa vērtību scanner, String tipa
  // vērtību prompt un atgriež String tipa vērtību line
  private static String readLineOrExit(Scanner scanner, String prompt) throws Exception {
    System.out.print(prompt);
    String line = scanner.nextLine();
    if (line != null && line.trim().equalsIgnoreCase("iziet")) {
      throw new Exception("Reģistrācija atcelta lietotāja pieprasījumā!");
    }
    return line;
  }

  // funkcija phoneNumberExists pieņem HashMap<String, User> tipa vērtību users,
  // String tipa vērtību phoneNumber un atgriež boolean tipa vērtību exists
  private static boolean phoneNumberExists(HashMap<String, User> users, String phoneNumber) {
    for (User elem : users.values()) {
      if (elem.phoneNumber != null && elem.phoneNumber.equals(phoneNumber))
        return true;
    }
    return false;
  }

  // funkcija changePassword pieņem Scanner tipa vērtību scanner, HashMap<String,
  // User> tipa vērtību users
  public void changePassword(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.print("Ievadi pašreizējo paroli: ");
    String curr_pwd = scanner.nextLine();
    if (!curr_pwd.equals(this.password)) {
      throw new Exception("Nepareiza parole!");
    }

    System.out.print("Izdomā jaunu paroli (8 simboli, mazie burti, lielie burti): ");
    String pwd = scanner.nextLine();
    if (!validatePassword(pwd)) {
      throw new Exception(
          "Parolei jābut vismaz 8 simbolu garai, saturet mazos burtus, lielos burtus un ciparus!");
    }

    System.out.print("Ievadi paroli vēlreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrīt");
    }

    this.password = pwd;
    users.put(this.email, this);
  }

  private static final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  private static final Pattern STRONG = Pattern.compile("^(?=.{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

  // funkcija validatePassword pieņem String tipa vērtību pwd un atgriež boolean
  // tipa vērtību
  private static boolean validatePassword(String pwd) {
    return STRONG.matcher(pwd).matches();
  }

  // funkcija validateEmail pieņem String tipa vērtību email un atgriež boolean
  // tipa vērtību
  private static boolean validateEmail(String email) {

    return EMAIL_RE.matcher(email).matches();
  }

  // funkcija toCSV atgriež String tipa vērtību
  public String toCSV() {
    return email + "," + password + "," + role.name() + "," + phoneNumber + "," + autoType.name() + "\n";
  }

  // funkcija checkPassword pieņem String tipa vērtību check un atgriež boolean
  // tipa vērtību
  public boolean checkPassword(String check) {
    return password.equals(check);
  }

  // funkcija toTableRow pieņem List<Integer> tipa vērtību widths un atgriež
  // String tipa vērtību tableRow
  public String toTableRow(List<Integer> widths) {
    String formatString = widths.stream()
        .map(w -> "%-" + w + "s")
        .collect(Collectors.joining(" | ", "| ", " |"));

    return String.format(formatString, email, role, phoneNumber, autoType);
  }

  // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa
  // vērtību
  public static User fromCSV(String csvdata) throws Exception {
    // email,password,role
    String[] fields = csvdata.split(",");
    if (fields.length < 2) {
      throw new Exception("Nepareizs csv datu skaits: saņēma " + fields.length + ", gaidīja vismaz 2");
    }
    String email = fields[0];
    String name = fields[1];

    UserRole role = UserRole.User;
    if (fields.length >= 3) {
      role = UserRole.valueOf(fields[2]);
    }

    String phoneNumber = "";
    if (fields.length >= 4) {
      phoneNumber = fields[3];
    }

    AutoType autoType = AutoType.Any;
    if (fields.length >= 5) {
      autoType = AutoType.valueOf(fields[4]);
    }

    return new User(email, name, role, phoneNumber, autoType);
  }
}
