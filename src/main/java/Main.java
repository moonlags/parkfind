public class Main {
  public static void main(String[] args) {
    // Izveido UserInterface klases objektu
    // Pieņemam, ka šī klase satur visu lietotāja mijiedarbības loģiku
    UserInterface ui = new UserInterface();

    System.out.println(Security.hashPassword("root"));
    // Palaiž programmu
    // run() metodē, visticamāk, atrodas galvenais programmas cikls vai darbība
    ui.run();
  }
}
