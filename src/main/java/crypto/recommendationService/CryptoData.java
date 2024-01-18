package crypto.recommendationService;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bean class representing the crypto data
 * The class implements the Comparable interface for sorting the cryptoData's descending by the normalized range
 */
public class CryptoData implements Comparable<CryptoData>{
	/**
	 * The crypto code
	 */
	private String code;
	/**
	 * The start date of the period for which this data is computed
	 */
	private LocalDateTime startTime;
	/**
	 * The end date of the period for which this data is computed
	 */
	private LocalDateTime endTime;
	/**
	 * The oldest price in the period
	 */
	private double oldest;
	/**
	 * The newest price in the period
	 */
	private double newest;
	/**
	 * The minimum price in the period
	 */
	private double min;
	/**
	 * The maximum price in the period
	 */
	private double max;
	/**
	 * The normalized range that will be computed in base of the minimum and maximum price ((max - min) / min)
	 */
	private double normalizedRange;
	
	/**
	 * Constructor for the crypto data
	 * @param code the crypto code
	 * @param startTime the start of the period
	 * @param endTime the end of the period
	 * @param oldest the oldest price
	 * @param newest the newest price
	 * @param min the minimum price
	 * @param max the maximum price
	 */
	public CryptoData(String code,
					  LocalDateTime startTime, 
					  LocalDateTime endTime,
					  double oldest, double newest,
					  double min, double max) {
		this.setCode(code);
		this.setStartTime(startTime);
		this.setEndTime(endTime);
		this.setOldest(oldest);
		this.setNewest(newest);
		this.setMin(min);
		this.setMax(max);
		this.setNormalizedRange();
	}
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public double getOldest() {
		return oldest;
	}

	public void setOldest(double oldest) {
		this.oldest = oldest;
	}

	public double getNewest() {
		return newest;
	}

	public void setNewest(double newest) {
		this.newest = newest;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getNormalizedRange() {
		//compute the normalized range every time we get it, in case the minimum or maximum values changed
		this.setNormalizedRange();
		return this.normalizedRange;
	}

	public void setNormalizedRange() {
		this.normalizedRange = (this.max - this.min) / this.min;
	}

	@Override
	public int compareTo(CryptoData o) {
		if (this.getNormalizedRange() > o.getNormalizedRange()) return -1;
		else if (this.getNormalizedRange() < o.getNormalizedRange()) return 1;
		else return 0;
	}
	
	@Override
	public String toString() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		NumberFormat nf = new DecimalFormat("#0.00"); 
		return " Code : " + this.getCode() +
			   " | Period : " + this.getStartTime().toLocalDate().format(dtf) + " - " + this.getEndTime().toLocalDate().format(dtf) +
			   " | Oldest Price : " + nf.format(this.getOldest()) +
			   " | Newest Price : " + nf.format(this.getNewest()) +
			   " | Min Price : " + nf.format(this.getMin()) +
			   " | Max Price : " + nf.format(this.getMax()) +
			   " | Normalized Range : " + nf.format(this.getNormalizedRange());
	}
}
