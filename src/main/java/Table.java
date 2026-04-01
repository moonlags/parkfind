// import java.util.List;

// TODO: need to implement this for user, park, rate
// interface TablePrintable {
// 	String toTableRow(List<Integer> max_widths);
// }

public class Table {
	// TODO: add arguments, implement logic
	// maybe use printf with %-15s format where 15 is maximum width of a column
	// EXAMPLE ARGUMENTS!!!
	// public static void printTable(List<String> column_names, List<Integer>
	// max_column_widths,
	// List<TablePrintable> elems) {
	//
	// }
}

// TODO: EXAMPLE USER IMPLEMENTATION
// public String toTableRow(List<Integer> widths) {
// // Создаем строку формата: %-10s %-20s %-10s
// String formatString = widths.stream()
// .map(w -> "%-" + w + "s")
// .collect(Collectors.joining(" | ", "| ", " |%n"));
//
// String.format(formatString, email, role);
// }

// TODO: USAGE EXAMPLE
// public static void main(String[] args) {
// RowPrinter printer = new ConsoleRowPrinter();
//
// // 1. Конфигурируем ширину колонок
// List<Integer> columns = List.of(10, 15, 5);
// HandlerFn rowHandler = printer.invoke(columns);
//
// // 2. Печатаем строки, используя полученный хендлер
// rowHandler.handle("ID", "Name", "Age");
// rowHandler.handle("1", "John Doe", "30");
// rowHandler.handle("2", "Jane Smith", "25");
// }
