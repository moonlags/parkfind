import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

@FunctionalInterface
interface ParserFn<T> {
  // funkcija invoke pieņem String tipa vērtību csvdata un atgriež T tipa vērtību
  T invoke(String csvdata) throws Exception;
}

public class FileHandler<T extends CSVEncodable> {
  private final String filepath;
  private final ParserFn<T> parser;

  // funkcija FileHandler pieņem String tipa vērtību filepath un ParserFn<T> tipa
  // vērtību parser un atgriež:
  public FileHandler(String filepath, ParserFn<T> parser) {
    this.filepath = filepath;
    this.parser = parser;
  }

  // funkcija loadAll atgriež ArrayList<T> tipa vērtību result
  public ArrayList<T> loadAll() throws IOException {
    File f = new File(filepath);
    // ja fails neeksistē, tas tiek izveidots
    if (!f.exists())
      f.createNewFile();

    try (Scanner scanner = new Scanner(f)) {
      ArrayList<T> result = new ArrayList<T>();

      // cikls nolasa katru faila rindu un mēģina to pārvērst par T tipa objektu
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        try {
          result.add(parser.invoke(line));
          // kļūdu apstrāde gadījumā, ja parsēšana neizdodas
        } catch (Exception e) {
          Color.warn("Neizdevās ieladēt csv objektu: " + e.getMessage());
        }
      }

      scanner.close();
      return result;
    }
  }

  // funkcija appendOne pieņem T tipa vērtību data un atgriež:
  public void appendOne(T data) throws IOException {
    FileWriter w = new FileWriter(filepath, true);
    // ieraksta vienu elementu faila beigās CSV formātā
    w.write(data.toCSV());

    w.close();
  }

  // funkcija writeAll pieņem Collection<T> tipa vērtību data un atgriež:
  public void writeAll(Collection<T> data) throws IOException {
    FileWriter w = new FileWriter(filepath, false);
    // cikls pārraksta failu ar visiem kolekcijas elementiem CSV formātā
    for (T elem : data) {
      w.write(elem.toCSV());
    }

    w.close();
  }
}
