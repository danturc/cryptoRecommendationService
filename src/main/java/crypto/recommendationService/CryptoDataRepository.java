package crypto.recommendationService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

/**
 * Interface for database operations with crypto data's
 */
public interface CryptoDataRepository extends CrudRepository<CryptoData, Integer> {
	/**
	 * Finds crypto data with a specific code, and time period
	 * @param code the code to be searched for
	 * @param startTime the start time of the period
	 * @param endTime the end time of the period
	 * @return a list of found crypto data
	 */
	public List<CryptoData> findByCodeAndStartTimeAndEndTime(String code, LocalDateTime startTime, LocalDateTime endTime);
	
	/**
	 * Finds crypto data with a specific code and also with the start time greater than a given time
	 * @param code the code to be searched for
	 * @param historyTime the given time
	 * @return a list of found crypto data
	 */
	public List<CryptoData> findByCodeAndStartTimeGreaterThan(String code, LocalDateTime historyTime);
}
