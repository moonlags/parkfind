public class Main {
  public static void main(String[] args) {
    System.setProperty("file.encoding", "UTF-8");

    // Izveido UserInterface klases objektu
    // Pieņemam, ka šī klase satur visu lietotāja mijiedarbības loģiku
    UserInterface ui = new UserInterface();

    // Palaiž programmu
    // run() metodē, visticamāk, atrodas galvenais programmas cikls vai darbība
    ui.run();
  }
}
