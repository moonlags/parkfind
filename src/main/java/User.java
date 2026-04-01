// TODO: visu izvadi latviesu valoda

import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

enum UserRole {
  User,
  Admin
}

public class User implements CSVEncodable {
  private String email;
  private String password;
  private UserRole role;
  private String phoneNumber;
  private AutoType autoType;

  private User(String email, String password, UserRole role, String phoneNumber) {
    this.email = email;
    this.password = password;
    this.role = role;
    this.phoneNumber = phoneNumber;
  }

  // funkcija role atgriež UserRole tipa vērtību role
  public UserRole role() {
    return role;
  }

  // funkcija email atgriež String tipa vērtību email
  public String email() {
    return email;
  }

  // funkcija login pieņem Scanner tipa vērtību scanner, HashMap<String, User> tipa vērtību users un
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

  // funkcija register pieņem Scanner tipa vērtību scanner, HashMap<String, User> tipa vērtību users
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
          "Parolei jābūt vismaz 8 simbolu garai, saturēt mazos burtus, lielos burtus un"
              + " ciparus!");
    }

    System.out.print("Ievadi paroli vēlreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrīt");
    }

    System.out.println("Ievadi tālruņa numuru: ");

    User user = new User(email, pwd, UserRole.User);

    users.put(email, user);
    return user;
  }

  // funkcija changePassword pieņem Scanner tipa vērtību scanner, HashMap<String, User> tipa vērtību
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
          "Parolei jābut vismaz 8 simbolu garai, saturet mazos burtus, lielos burtus un"
              + " ciparus!");
    }

    System.out.print("Ievadi paroli vēlreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrīt");
    }

    this.password = pwd;
    users.put(this.email, this);
  }

  // funkcija validatePassword pieņem String tipa vērtību pwd un atgriež boolean tipa vērtību
  private static boolean validatePassword(String pwd) {
    final Pattern STRONG = Pattern.compile("^(?=.{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
    return STRONG.matcher(pwd).matches();
  }

  // funkcija validateEmail pieņem String tipa vērtību email un atgriež boolean tipa vērtību
  private static boolean validateEmail(String email) {
    final Pattern EMAIL_RE = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    return EMAIL_RE.matcher(email).matches();
  }

  // private String email;
  // private String password;
  // private boolean isAdmin;
  // private LocalDateTime lastLoginDate;
  // private LocalDateTime creationDate;
  // private int activity;

  // funkcija toCSV atgriež String tipa vērtību
  public String toCSV() {
    return email + "," + password + "," + role + "\n";
  }

  // funkcija print neko neatgriež un neko nepienem
  public void print(int width) {
    System.out.printf("E-pasts: %-" + width + "s; Loma: %-5s\n", email, role);
  }

  // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa vērtību
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
      role = UserRole.valueOf(fields[2]);
    }

    return new User(email, name, role);
  }
}
