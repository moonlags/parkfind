// TODO: visu izvadi latviesu valoda

import java.util.HashMap;
import java.util.Scanner;
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

public class User implements CSVEncodable {
  private String email;
  private String password;
  private UserRole role;
  private String phoneNumber;
  private AutoType autoType;

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

  // funkcija email atgriež String tipa vērtību email
  public String email() {
    return email;
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
  // tipa vērtību users
  // un atgriež User tipa vērtību user
  public static User register(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.print("Ievadi e-pastu: ");
    String email = scanner.nextLine();
    if (!validateEmail(email)) {
      throw new Exception("Nepareizs e-pasta formāts!");
    }

    if (users.containsKey(email)) {
      throw new Exception("E-pasts ir jau izmantots!");
    }

    System.out.print("Izdomā paroli: ");
    String pwd = scanner.nextLine();
    if (!validatePassword(pwd)) {
      throw new Exception(
          "Parolei jābūt vismaz 8 simbolu garai, saturēt mazos burtus, lielos burtus un ciparus!");
    }

    System.out.print("Ievadi paroli vēlreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrīt!");
    }

    System.out.print("Ievadi tālruņa numuru: ");
    String phoneNumber = scanner.nextLine();
    if (phoneNumber.charAt(0) != '+' || phoneNumber.length() != 12) {
      throw new Exception("Nepareizs tālruņa numura formāts!");
    }

    User user = new User(email, pwd, UserRole.User, phoneNumber, AutoType.Any);

    users.put(email, user);
    return user;
  }

  // funkcija changePassword pieņem Scanner tipa vērtību scanner, HashMap<String,
  // User> tipa vērtību
  // users
  public void changePassword(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.print("Ievadi pašreizējo paroli: ");
    String curr_pwd = scanner.nextLine();
    if (!curr_pwd.equals(this.password)) {
      throw new Exception("Nepareiza parole!");
    }

    System.out.print("Izdomā jaunu paroli: ");
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

  // funkcija validatePassword pieņem String tipa vērtību pwd un atgriež boolean
  // tipa vērtību
  private static boolean validatePassword(String pwd) {
    final Pattern STRONG = Pattern.compile("^(?=.{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
    return STRONG.matcher(pwd).matches();
  }

  // funkcija validateEmail pieņem String tipa vērtību email un atgriež boolean
  // tipa vērtību
  private static boolean validateEmail(String email) {
    final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    return EMAIL_RE.matcher(email).matches();
  }

  // funkcija toCSV atgriež String tipa vērtību
  public String toCSV() {
    return email + "," + password + "," + role.ordinal() + "," + phoneNumber + "," + autoType.ordinal() + "\n";
  }

  // funkcija print neko neatgriež un neko nepienem
  public void print(int width) {
    System.out.printf("E-pasts: %-" + width + "s; Loma: %-14s; Tālruņa numurs: %s; Automašīnas tips: %-12s\n", email,
        role, phoneNumber, autoType);
  }

  // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa
  // vērtību
  public static User fromCSV(String csvdata) throws Exception {
    // email,password,role
    String[] fields = csvdata.split(",");
    if (fields.length < 2) {
      throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 2");
    }
    String email = fields[0];
    String name = fields[1];

    UserRole role = UserRole.User;
    if (fields.length >= 3) {
      int i = Integer.valueOf(fields[2]);
      role = UserRole.values()[i];
    }

    String phoneNumber = "";
    if (fields.length >= 4) {
      phoneNumber = fields[3];
    }

    AutoType autoType = AutoType.Any;
    if (fields.length >= 5) {
      int i = Integer.valueOf(fields[4]);
      autoType = AutoType.values()[i];
    }

    return new User(email, name, role, phoneNumber, autoType);
  }
}
