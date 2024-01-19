package crypto.recommendationService;

import java.util.List;
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
	@GetMapping("/crypto/print/all")
	public String printAllCryptoData() {
		return service.parseCryptoCSVFolder().stream().map(CryptoData::toString).collect(Collectors.joining("<br>"));
	}
	
	/**
	 * Gets all the crypto data from all the files in the prices folder sorted in descending order 
	 * according to the normalized range
	 * @return a set of crypto data with oldest/newest/min/max prices 
	 */
	@GetMapping("/crypto/get/all")
	public Set<CryptoData> getAllCryptoData() {
		return service.parseCryptoCSVFolder();
	}
	
	/**
	 * Gets the crypto data for a specific crypto code 
	 * @param code The crypto code to identify the prices file
	 * @return crypto data with oldest/newest/min/max prices 
	 */
	@GetMapping("/crypto/get/code/{code}")
	public CryptoData getCryptoDataByCode(@PathVariable(value="code") String code) {
		return service.parseCryptoCSVFile(code);
	}
	
	/**
	 * Prints the crypto data for a specific crypto code 
	 * @param code The crypto code to identify the prices file
	 * @return crypto data string representation with oldest/newest/min/max prices 
	 */
	@GetMapping("/crypto/print/code/{code}")
	public String printCryptoDataByCode(@PathVariable(value="code") String code) {
		return service.parseCryptoCSVFile(code).toString();
	}
	
	/**
	 * Gets the crypto data from all the prices files with the highest normalized range for a specific day
	 * @param date the day to be searched for
	 * @return crypto data with oldest/newest/min/max prices
	 */
	@GetMapping("/crypto/get/date/{date}")
	public CryptoData getCryptoDataByDate(@PathVariable(value="date") String date) {
		return service.getHighestCryptoForDay(date);
	}
	
	/**
	 * Prints the crypto data from all the prices files with the highest normalized range for a specific day
	 * @param date the day to be searched for
	 * @return crypto data string representation with oldest/newest/min/max prices
	 */
	@GetMapping("/crypto/print/date/{date}")
	public String printCryptoDataByDate(@PathVariable(value="date") String date) {
		return service.getHighestCryptoForDay(date).toString();
	}
	
	/**
	 * Gets all the supported crypto codes
	 * @return a list containing all the supported crypto codes
	 */
	@GetMapping("/crypto/get/codes")
	public List<Code> getAllCodes() {
		return service.getAllCryptoCodes();
	}
	
	/**
	 * Prints all the supported crypto codes
	 * @return a String with all the supported crypto codes concatenated
	 */
	@GetMapping("/crypto/print/codes")
	public String printAllCodes() {
		return service.getAllCryptoCodes().stream().map(Code::toString).collect(Collectors.joining("<br>"));
	}
	
	/**
	 * Adds a new crypto code to be supported
	 * @param code the new code to be added
	 * @return the new added crypto code
	 */
	@GetMapping("/crypto/add/code/{code}")
	public Code addNewCode(@PathVariable(value="code") String code) {
		return service.addCryptoCode(code);
	}
	
	/**
	 * Gets all the crypto data from history for a given number of months ago
	 * @param months the number of months
	 * @return a set of merged crypto data for the last months period for every supported crypto code 
	 */
	@GetMapping("/crypto/get/history/all/{months}")
	public Set<CryptoData> getAllCryptoDataFromHistory(@PathVariable(value="months") String months) {
		return service.getAllCryptoDataFromHistory(months);
	}
	
	/**
	 * Prints all the crypto data from history for a given number of months ago
	 * @param months the number of months
	 * @return a string containing all the merged crypto data for the last months period for every supported crypto code
	 */
	@GetMapping("/crypto/print/history/all/{months}")
	public String printAllCryptoDataFromHistory(@PathVariable(value="months") String months) {
		return service.getAllCryptoDataFromHistory(months).stream().map(CryptoData::toString).collect(Collectors.joining("<br>"));
	}
	
	/**
	 * Gets the merged crypto data for a specific number of months ago and for a specific crypto code
	 * @param code the crypto code to be searched for
	 * @param months the number of months
	 * @return a merged crypto data for the last number of months ago and for a specific crypto code
	 */
	@GetMapping("/crypto/get/history/code/{code}/{months}")
	public CryptoData getCryptoDataByCodeFromHistory(@PathVariable(value="code") String code, @PathVariable(value="months") String months) {
		return service.getCryptoDataByCodeFromHistory(code, months);
	}
	
	/**
	 * Prints the merged crypto data for a specific number of months ago and for a specific crypto code
	 * @param code the crypto code to be searched for
	 * @param months the number of months
	 * @return a string representation of the merged crypto data for the last number of months ago 
	 * and for a specific crypto code
	 */
	@GetMapping("/crypto/print/history/code/{code}/{months}")
	public String printCryptoDataByCodeFromHistory(@PathVariable(value="code") String code, @PathVariable(value="months") String months) {
		return service.getCryptoDataByCodeFromHistory(code, months).toString();
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
