import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchResult implements TablePrintable {
	private Park park;
	private Rate rate;
	private double price;
	private List<Rate> usedRates;

	SearchResult(Park park, Rate rate, double price) {
		this.park = park;
		this.rate = rate;
		this.price = price;
		this.usedRates = new ArrayList<>();
		this.usedRates.add(rate);
	}

	SearchResult(Park park, Rate rate, double price, List<Rate> usedRates) {
		this.park = park;
		this.rate = rate;
		this.price = price;
		this.usedRates = usedRates;
	}

	SearchResult(Rate rate, double price, List<Rate> usedRates) {
		this.rate = rate;
		this.price = price;
		this.usedRates = usedRates;
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

	public List<Rate> usedRates() {
		return usedRates;
	}

	public String toTableRow(List<Integer> widths) {
		String formatString = widths.stream()
				.map(w -> "%-" + w + "s")
				.collect(Collectors.joining(" | ", "| ", " |"));

		String rateLabel = usedRates.stream()
				.map(r -> r.rateType().toString())
				.distinct()
				.collect(Collectors.joining("+"));

		return String.format(formatString, park.address(), rateLabel, String.format("%.2f", price));
	}
}
