public class Color {

    // ANSI bēgšanas kodi, kas liek terminālim mainīt teksta krāsu
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BLACK = "\u001B[30m";
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String REVERSED = "\u001B[7m";
    public static final String BG_RED = "\u001B[41m";

    // funkcija printRed pieņem String tipa vērtību sarkanāTekstaRinda un neatgriež
    // nekādu vērtību
    public static void error(String text) {
        System.out.println(RED + text + RESET);
    }

    // funkcija printGreen pieņem String tipa vērtību zaļāTekstaRinda un neatgriež
    // nekādu vērtību
    public static void success(String text) {
        System.out.println(GREEN + text + RESET);
    }

    // funkcija printYellow pieņem String tipa vērtību dzeltenāTekstaRinda un
    // neatgriež nekādu vērtību
    public static void warn(String text) {
        System.out.println(YELLOW + text + RESET);
    }
}
