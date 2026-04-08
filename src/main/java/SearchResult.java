import java.util.List;
import java.util.stream.Collectors;

public class SearchResult implements TablePrintable {
	private Park park;
	private Rate rate;
	private double price;

	SearchResult(Park park, Rate rate, double price) {
		this.park = park;
		this.rate = rate;
		this.price = price;
	}

	public double price() {
		return price;
	}

	public Park park() {
		return park;
	}

	public Rate rate() {
		return rate;
	}

	public String toTableRow(List<Integer> widths) {
		String formatString = widths.stream()
				.map(w -> "%-" + w + "s")
				.collect(Collectors.joining(" | ", "| ", " |"));

		return String.format(formatString, park.address(), rate.rateType(), price);
	}
}
