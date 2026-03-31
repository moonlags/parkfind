@FunctionalInterface
public interface ParserFn<T> {
  // funkcija invoke pieņem String tipa vērtību csvdata un atgriež T tipa vērtību
  T invoke(String csvdata) throws Exception;
}
