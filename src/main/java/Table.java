import java.util.List;
import java.util.stream.Collectors;

interface TablePrintable {
	String toTableRow(List<Integer> max_widths);
}

public class Table {
	// funkcija printTable pieņem List<String> tipa vērtību columnNames, List<Integer> tipa vērtību maxColumnWidths, 
	// List<TablePrintable> tipa vērtību elems un atgriež nevienu vērtību 
	public static void printTable(List<String> columnNames, List<Integer> maxColumnWidths,
			List<TablePrintable> elems) {
		String separator = separator(maxColumnWidths);
		String formatString = maxColumnWidths.stream()
				.map(w -> "%-" + w + "s")
				.collect(Collectors.joining(" | ", "| ", " |%n"));

		System.out.println(separator);
		System.out.printf(formatString, columnNames.toArray());

		for (TablePrintable row : elems) {
			System.out.println(separator);
			System.out.println(row.toTableRow(maxColumnWidths));
		}
		System.out.println(separator);
	}

	// funkcija separator pieņem List<Integer> tipa vērtību widths un atgriež String tipa vērtību result
	private static String separator(List<Integer> widths) {
		String result = "+";
		for (int i = 0; i < widths.size(); i++) {
			for (int j = 0; j < widths.get(i) + 2; j++) {
				result += "-";
			}
			result += "+";
		}
		return result;
	}
}
