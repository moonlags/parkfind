// TODO: visu izvadi latviesu valoda

import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

public class User implements CSVEncodable {
  private String email;
  private String password;
  private boolean isAdmin; // TODO: enum(lietotajs, administrators)

  private User(String email, String password, boolean isAdmin) {
    this.email = email;
    this.password = password;
    this.isAdmin = isAdmin;
  }

  // funkcija isAdmin atgriež boolean tipa vērtību isAdmin
  public boolean isAdmin() {
    return isAdmin;
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
      throw new Exception("Nepareizs e-pasta formats!");
    }

    if (users.containsKey(email)) {
      throw new Exception("E-pasts ir jau izmantots!");
    }

    System.out.print("Izdoma paroli: ");
    String pwd = scanner.nextLine();
    if (!validatePassword(pwd)) {
      throw new Exception(
          "Parolei jabut vismazak 8 simbolu garai, saturet mazus burtus, lielus burtus un"
              + " ciparus!");
    }

    System.out.print("Ievadi paroli velreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrit");
    }

    User user = new User(email, pwd, false);

    users.put(email, user);
    return user;
  }

  // funkcija changePassword pieņem Scanner tipa vērtību scanner, HashMap<String, User> tipa vērtību
  // users
  public void changePassword(Scanner scanner, HashMap<String, User> users) throws Exception {
    System.out.print("Ievadi pasreizejo paroli: ");
    String curr_pwd = scanner.nextLine();
    if (!curr_pwd.equals(this.password)) {
      throw new Exception("Nepareiza parole!");
    }

    System.out.print("Izdoma jaunu paroli: ");
    String pwd = scanner.nextLine();
    if (!validatePassword(pwd)) {
      throw new Exception(
          "Parolei jabut vismazak 8 simbolu garai, saturet mazus burtus, lielus burtus un"
              + " ciparus!");
    }

    System.out.print("Ievadi paroli velreiz: ");
    String pwd2 = scanner.nextLine();
    if (!pwd.equals(pwd2)) {
      throw new Exception("Paroles nesakrit");
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
    return email + "," + password + "," + isAdmin + "\n";
  }

  // funkcija print neko neatgriež un neko nepienem
  public void print(int width) {
    System.out.printf("E-pasts: %-" + width + "s; Admins: %-5s\n", email, isAdmin);
  }

  // funkcija fromCSV pieņem String tipa vērtību csvdata un atgriež User tipa vērtību
  public static User fromCSV(String csvdata) throws Exception {
    // email,password,isAdmin,lastLoginDate,creationDate,activity
    String[] fields = csvdata.split(",");
    if (fields.length < 2) {
      throw new Exception("Invalid csv fields: got " + fields.length + " expected atleast 2");
    }
    String email = fields[0];
    String name = fields[1];

    boolean isAdmin = false;
    if (fields.length >= 3) {
      isAdmin = Boolean.valueOf(fields[2]);
    }

    return new User(email, name, isAdmin);
  }
}
