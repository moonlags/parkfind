import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public class FileHandler<T extends CSVEncodable> {
  private final String filepath;
  private final ParserFn<T> parser;

  public FileHandler(String filepath, ParserFn<T> parser) {
    this.filepath = filepath;
    this.parser = parser;
  }

  // funkcija loadAll atgriež ArrayList<T> tipa vērtību result
  public ArrayList<T> loadAll() throws IOException {
    File f = new File(filepath);
    if (!f.exists()) f.createNewFile();

    Scanner scanner = new Scanner(f);
    ArrayList<T> result = new ArrayList<T>();

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();

      try {
        result.add(parser.invoke(line));
      } catch (Exception e) {
        System.err.println("Error while loading from file: " + e.getMessage());
      }
    }
    scanner.close();

    return result;
  }

  public void appendOne(T data) throws IOException {
    FileWriter w = new FileWriter(filepath, true);

    w.write(data.toCSV());

    w.close();
  }

  public void writeAll(Collection<T> data) throws IOException {
    FileWriter w = new FileWriter(filepath, false);

    for (T elem : data) {
      w.write(elem.toCSV());
    }

    w.close();
  }
}
