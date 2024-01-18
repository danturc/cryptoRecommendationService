package crypto.recommendationService;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class that calls the business methods from the service class and forwards the model to the frontend
 */
@RestController
public class RecommendationServiceController {
	/**
	 * Injected service object
	 */
	@Autowired
	private CryptoCSVService service;
	
	/**
	 * Prints all the crypto data from all the files in the prices folder sorted in descending order 
	 * according to the normalized range
	 * @return a string with the list of crypto data with oldest/newest/min/max prices 
	 */
	@GetMapping("/crypto/print_all")
	public String printAllCryptoData() {
		return service.parseCryptoCSVFolder().stream().map(CryptoData::toString).collect(Collectors.joining("<br>"));
	}
	
	/**
	 * Gets all the crypto data from all the files in the prices folder sorted in descending order 
	 * according to the normalized range
	 * @return a set of crypto data with oldest/newest/min/max prices 
	 */
	@GetMapping("/crypto/get_all")
	public Set<CryptoData> getAllCryptoData() {
		return service.parseCryptoCSVFolder();
	}
	
	/**
	 * Gets the crypto data for a specific crypto code 
	 * @param code The crypto code to identify the prices file
	 * @return crypto data with oldest/newest/min/max prices 
	 * @throws RecommendationServiceException when the prices file is not found, corrupted or IO error occurs
	 */
	@GetMapping("/crypto/get_by_code/{code}")
	public CryptoData getCryptoDataByCode(@PathVariable(value="code") String code) {
		return service.parseCryptoCSVFile(code);
	}
	
	/**
	 * Prints the crypto data for a specific crypto code 
	 * @param code The crypto code to identify the prices file
	 * @return crypto data string representation with oldest/newest/min/max prices 
	 * @throws RecommendationServiceException when the prices file is not found, corrupted or IO error occurs
	 */
	@GetMapping("/crypto/print_by_code/{code}")
	public String printCryptoDataByCode(@PathVariable(value="code") String code) {
		return service.parseCryptoCSVFile(code).toString();
	}
	
	/**
	 * Gets the crypto data from all the prices files with the highest normalized range for a specific day
	 * @param date the day to be searched for
	 * @return crypto data with oldest/newest/min/max prices
	 * @throws RecommendationServiceException when the date parameter is not correct or there is no crypto data for this specific day
	 */
	@GetMapping("/crypto/get_by_date/{date}")
	public CryptoData getCryptoDataByDate(@PathVariable(value="date") String date) {
		return service.getHighestCryptoForDay(date);
	}
	
	/**
	 * Prints the crypto data from all the prices files with the highest normalized range for a specific day
	 * @param date the day to be searched for
	 * @return crypto data string representation with oldest/newest/min/max prices
	 * @throws RecommendationServiceException when the date parameter is not correct or there is no crypto data for this specific day
	 */
	@GetMapping("/crypto/print_by_date/{date}")
	public String printCryptoDataByDate(@PathVariable(value="date") String date) {
		return service.getHighestCryptoForDay(date).toString();
	}
	
	/**
	 * Method for handling the exceptions, all the methods in the service throw the same runtime exception 
	 * RecommendationServiceException
	 * @param re the runtime exception
	 * @return the message of the exception
	 */
	@ExceptionHandler({ RecommendationServiceException.class })
    public String handleException(RecommendationServiceException re) {
        return re.getMessage();
    }
}
