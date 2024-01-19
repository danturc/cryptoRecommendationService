package crypto.recommendationService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import jakarta.transaction.Transactional;

/**
 * Service Class containing the business logic for parsing data from the CSV crypto prices files
 * The class contains methods for parsing all the files from the folder, a specific crypto code or a specific day
 */
@Service
public class CryptoCSVService {
	/**
	 * Repository for the codes table
	 */
	@Autowired
	private CodeRepository codeRepository;
	
	/**
	 * Repository for the crypto_datas table
	 */
	@Autowired
	private CryptoDataRepository cryptoDataRepository;

	/**
	 * The name of the folder containing the CSV crypto prices files
	 */
	private Path cryptoPricesFolder;
	
	/**
	 * The suffix before the code of the CSV crypto prices files
	 */
	@Value("${crypto.file.suffix}")
	private String cryptoFileSuffix;
	
	/**
	 * Constructor used to set the crypto prices folder name in base of the classpath
	 * @param cryptoPricesFolderPath
	 */
	public CryptoCSVService(@Value("${crypto.prices.folder}") String cryptoPricesFolderPath) {
		try {
			//if classpath present in the application.properties remove it
			this.cryptoPricesFolder = cryptoPricesFolderPath.startsWith("classpath:")?
					  				  Path.of(getClass().getResource(cryptoPricesFolderPath.replace("classpath:", "")).toURI()):
					  				  Path.of(cryptoPricesFolderPath);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RecommendationServiceException("Unknown error retrieving the crypto prices folder", e);
		}
	}
	
	/**
	 * This method returns all the crypto codes found in the valid file names from the prices folder 
	 * @return A set of crypto codes
	 * @deprecated replaced by the method getAllCryptoCodes that take the codes from database
	 */
	private Set<String> parseCryptoCodesInFolder() {
		//stream all the files from the prices directory
		//filtering only files that ends with the correct suffix
		//that have a crypto code as a prefix
		try {
			return Files.list(cryptoPricesFolder)
				      .filter(file -> !Files.isDirectory(file))
				      .map(Path::getFileName)
				      .map(Path::toString)
				      .filter(file -> file.endsWith(cryptoFileSuffix))
				      .filter(file -> file.length() > cryptoFileSuffix.length())
				      .map(file -> file.substring(0, file.length() - cryptoFileSuffix.length()))
				      .collect(Collectors.toSet());
		} catch (IOException e) {
			throw new RecommendationServiceException("Error reading CSV crypto prices folder", e);
		}
	}
	
	/**
	 * This method parses a CSV file from the prices folder in base of a crypto code
	 * @param cryptoCode The code of the crypto to be parsed
	 * @return The crypto data containing oldest/newest/min/max prices taken from the CSV file 
	 */
	public CryptoData parseCryptoCSVFile(String cryptoCode) {
		return parseCryptoCSVFile(cryptoCode, null);
	}
	
	/**
	 * This method parses a CSV file from the prices folder in base of a crypto code and a given date
	 * @param cryptoCode The code of the crypto to be parsed
	 * @param date The date representing the day for which the data must be parsed, if this parameter is null 
	 * all the lines in the CSV file will be parsed otherwise only the lines having timestamps from this day
	 * @return The crypto data containing oldest/newest/min/max prices taken from the CSV file 
	 * or null if no data is found for a specific date
	 */
	public CryptoData parseCryptoCSVFile(String cryptoCode, LocalDate date) {
		//check if the code is present in the database
		cryptoCode = checkCryptoCode(cryptoCode);

		String fileName = cryptoCode + this.cryptoFileSuffix;
		String filePath = this.cryptoPricesFolder + File.separator + fileName;
		CryptoData cryptoData = null;
		CSVReader csvReader = null;
		try {
			//create the CSV reader and skip the header line
			csvReader = new CSVReader(new FileReader(filePath));
			csvReader.skip(1);
			String[] nextRecord;
			//init the parameters to be computed for the crypto data 
			Timestamp start = null;
			Timestamp end = null;
			double oldest = 0;
			double newest = 0;
			double min = Double.MAX_VALUE;
			double max = 0;
			boolean dataRead = false;
			//read the CSV file line by line
	        while ((nextRecord = csvReader.readNext()) != null) {
	        	//first set the parameters from the CSV line, timestamp, crypto code and price
	        	Timestamp time = new Timestamp(Long.valueOf(nextRecord[0].trim()));
	        	//in case the date is not null, it means that we have to take in considerations only the lines
	        	//having timestamps in this specific day of the date parameter otherwise we skip the line
	        	if (date != null && !time.toLocalDateTime().toLocalDate().equals(date)) 
	        		continue;
	        	String code = nextRecord[1].trim();
	        	double price = Double.valueOf(nextRecord[2].trim());
	        	//throw exception in case we find other crypto code than the one from the file name
	        	if (!code.equalsIgnoreCase(cryptoCode)) 
	        		throw new RecommendationServiceException("The crypto prices file is corrupted(other codes) : " + fileName);
	        	//throw exception if we find prices less or equal to zero
	        	if (price <= 0) 
	        		throw new RecommendationServiceException("The crypto prices file is corrupted(zero or negative prices) : " + fileName);
	        	//in case the timestamps are not in order we check every time for the lowest timestamp
	        	//updating the start date and oldest price for the crypto data
	        	if (start == null || time.before(start)) {
	        		start = time;
	        		oldest = price;
	        	}
	        	//in case the timestamps are not in order we check every time for the highest timestamp
	        	//updating the end date and newest price for the crypto data
	        	if (end == null || time.after(end)) {
	        		end = time;
	        		newest = price;
	        	}
	        	//update the minimum and maximum price
	        	if (price < min) 
	        		min = price;
	        	if (price > max) 
	        		max = price;
	        	//set a flag in case at least one line from the CSV file was read
	        	dataRead = true;
	        } 
	        //in case no data was red from the CSV file and the input date is null(we search all the records)
	        //it means the file is empty so we throw an exception
	        if (!dataRead && date == null) 
	        	throw new RecommendationServiceException("The crypto prices file is corrupted(no data) : " + fileName);
	        //if data was red from the file we create the crypto data to be returned 
	        //we make this check for the case where the date parameter(the specific day) is not null
	        //in that case the return crypto data will remain null
	        //we also save the crypto data in the database for later use in history checks
	        if (dataRead) 
	        	cryptoData = addCryptoData(new CryptoData(cryptoCode, start.toLocalDateTime(), end.toLocalDateTime(), oldest, newest, min, max));
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
			throw new RecommendationServiceException("The crypto prices file cannot be found : " + fileName, fe);
		} catch (CsvValidationException ce) {
			ce.printStackTrace();
			throw new RecommendationServiceException("The crypto prices file is corrupted : " + fileName, ce);
		} catch(NumberFormatException ne) {
			ne.printStackTrace();
			throw new RecommendationServiceException("The crypto prices file is corrupted(time or price format) : " + fileName, ne);
		} catch(ArrayIndexOutOfBoundsException ae) {
			ae.printStackTrace();
			throw new RecommendationServiceException("The crypto prices file is corrupted(insufficient data) : " + fileName, ae);
		} catch (IOException ie) {
			ie.printStackTrace();
			throw new RecommendationServiceException("IO error reading the crypto prices file : " + fileName, ie);
		} catch (RecommendationServiceException re) {
			throw re;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RecommendationServiceException("Unknown error reading the crypto prices file : " + fileName, e);
		} finally {
			//close the reader
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return cryptoData;
	}
	
	/**
	 * This method parses all the valid crypto prices files in the CSV folder 
	 * @return a set of crypto data for all the valid files 
	 */
	public Set<CryptoData> parseCryptoCSVFolder() {
		return parseCryptoCSVFolder(null);
	}
	
	/**
	 * This method parses all the valid crypto prices files in the CSV folder for a specific day
	 * returning a set sorted descending in base of the normalized range of the cryptos
	 * @param date A date representing the day filter for timestamps of the lines in the prices files
	 * @return A sorted set of parsed crypto data containing oldest/newest/min/max prices taken from the CSV file
	 */
	public Set<CryptoData> parseCryptoCSVFolder(LocalDate date) {
		//init the set to be returned as a sorted TreeSet that will keep 
		//the crypto data's sorted in base of the implemented compareTo method
		Set<CryptoData> cryptos = new TreeSet<>();
		//for every crypto code from the database
		for (Code code : getAllCryptoCodes()) {
			try {
				//parse the corresponding CSV file
				CryptoData cryptoData = parseCryptoCSVFile(code.getCode(), date);
				//if no data was found(this applies only when a date parameter is set) 
				//add the crypto data to the returning set
				if (cryptoData != null) 
					cryptos.add(cryptoData);
			} catch (RecommendationServiceException re) {
				//in case the CSV file is corrupted or an IO error occurs just ignore it
				re.printStackTrace();
			}
		}
		return cryptos;
	}
	
	/**
	 * This method gets the crypto data with the highest normalized range in a specific day
	 * @param date The day for which to search for records in the files
	 * @return The crypto data from the crypto with the highest normalized range in that specific day
	 */
	public CryptoData getHighestCryptoForDay(String date) throws RecommendationServiceException {
		LocalDate localDate = null;
		//first parse the string date parameter
		try {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			localDate = LocalDate.parse(date, dtf);
		} catch (DateTimeParseException de) {
			throw new RecommendationServiceException("Incorrect format for date parameter", de);
		}
		//get the descending sorted set in base of the normalized range for this specific day
		Set<CryptoData> cryptos = parseCryptoCSVFolder(localDate);
		//get the first element of the set , the one with the highest normalized range
		Optional<CryptoData> optional = cryptos.stream().findFirst();
		//throw an exception in case there is no data parsed in the set
		if (optional.isEmpty()) 
			throw new RecommendationServiceException("There is no crypto price data for this date");
		//otherwise return the crypto data with the highest normalized range
		return optional.get();
	}
	
	/**
	 * This method gets all the supported crypto codes from the database
	 * @return a list of crypto codes
	 */
	public List<Code> getAllCryptoCodes() {
		return (List<Code>) codeRepository.findAll();
	}
	
	/**
	 * This method adds a new crypto code in the database
	 * @param code the code to be added
	 * @return a new added Code object 
	 */
	@Transactional
	public Code addCryptoCode(String code) {
		//first check the code to be alphabetical and no longer than 5 characters
		//also check that the code does not already exists
		if (code == null) 
			throw new RecommendationServiceException("The crypto code must contain only alphabetic characters");
		code = code.trim().toUpperCase();
		if (codeRepository.existsByCode(code))
			throw new RecommendationServiceException("The crypto code " + code + " already exists");
		if (code.length() > 5)
			throw new RecommendationServiceException("The crypto code cannot have more than 5 characters");
		if (code.isEmpty() || !code.matches("^[a-zA-Z]*$"))
			throw new RecommendationServiceException("The crypto code must contain only alphabetic characters");
		//save the new code to the database and return it
		return codeRepository.save(new Code(code));
	}
	
	/**
	 * This method insert or update a new crypto data in the database
	 * @param cryptoData the new crypto data
	 * @return the updated/inserted crypto data from the database
	 */
	@Transactional
	public CryptoData addCryptoData(CryptoData cryptoData) {
		//check if there is already a record in the database for the corresponding code and period
		List<CryptoData> dataFromDb = cryptoDataRepository.findByCodeAndStartTimeAndEndTime(cryptoData.getCode(), cryptoData.getStartTime(), cryptoData.getEndTime());
		//if present in the database copy the prices from the new crypto data in the database record and prepare it for update
		//otherwise the new data will be inserted
		if (!dataFromDb.isEmpty()) {
			dataFromDb.get(0).copyData(cryptoData);
			cryptoData = dataFromDb.get(0);
		} 
		//insert or update the crypto data in the database
		return cryptoDataRepository.save(cryptoData);
	}
	
	/**
	 * This method creates a merged crypto data from all the crypto data in the database 
	 * for a specific code and a specific number of months from the moment of calling in the past 
	 * @param cryptoCode the code to be searched for
	 * @param nrOfMonthsStr the number of months
	 * @return a merged crypto data
	 */
	public CryptoData getCryptoDataByCodeFromHistory(String cryptoCode, String nrOfMonthsStr) {
		//check that the crypto code exists and the number of months is valid (between 1 and 36)
		cryptoCode = checkCryptoCode(cryptoCode);
		int nrOfMonths = checkMonths(nrOfMonthsStr);
		//calculate the time nrOfMonths ago
		LocalDateTime historyTime = LocalDateTime.now().minusMonths(nrOfMonths);
		//find all the crypto data for the given code with the start time after nrOfmonths ago
		List<CryptoData> historyCryptoDatas = cryptoDataRepository.findByCodeAndStartTimeGreaterThan(cryptoCode, historyTime);
		//throw exception if no crypto data was found
		if (historyCryptoDatas.isEmpty()) 
			throw new RecommendationServiceException("There is no data for the crypto " + cryptoCode + " in the last " + nrOfMonths + " months");
		//init the fields for the merged crypto data
		LocalDateTime start = null;
		LocalDateTime end = null;
		double oldest = 0;
		double newest = 0;
		double min = Double.MAX_VALUE;
		double max = 0;
		//for every crypto data found for this period
		for (CryptoData data : historyCryptoDatas) {
			//update the fields on every step
			if (start == null || data.getStartTime().isBefore(start)) {
        		start = data.getStartTime();
        		oldest = data.getOldest();
        	}
			if (end == null || data.getEndTime().isAfter(end)) {
        		end = data.getEndTime();
        		newest = data.getNewest();
        	}
        	if (data.getMin() < min) 
        		min = data.getMin();
        	if (data.getMax() > max) 
        		max = data.getMax();
		}
		//return the merged crypto data
		return new CryptoData(cryptoCode, start, end, oldest, newest, min, max);
	}
	
	/**
	 * This method returns a set of merged crypto data from the database for all the supported crypto codes  
	 * and for a specific number of months from the moment of calling in the past
	 * the returned set is sorted descending in base of the normalized range  
	 * @param nrOfMonthsStr the number of months 
	 * @return a sorted set of merged crypto data
	 */
	public Set<CryptoData> getAllCryptoDataFromHistory(String nrOfMonthsStr) {
		//check thath the number of months is valid (between 1 and 36)
		checkMonths(nrOfMonthsStr);
		
		//create the descending sorted set
		Set<CryptoData> hCryptoDataList = new TreeSet<>();
		//for every supported crypto code
		for (Code code : getAllCryptoCodes()) {
			//add the merged crypto data taken from database for the specified number of months ago
			//if there is no data found for this code ignore the exception
			try {
				hCryptoDataList.add(getCryptoDataByCodeFromHistory(code.getCode(), nrOfMonthsStr));
			} catch (RecommendationServiceException re) {}
		}
		//if no crypto data was found throw an exception with specific message
		if (hCryptoDataList.isEmpty()) 
			throw new RecommendationServiceException("There is no crypto data  in the last " + nrOfMonthsStr + " months");
		//return the created set
		return hCryptoDataList;
	}
	
	/**
	 * Private method for checking the number of months parameter that throws exceptions in case the check fails
	 * the number of months must be a number between 1 and 36
	 * @param nrOfMonthsStr the number of months to be checked as string
	 * @return the checked number of months as integer
	 */
	private int checkMonths(String nrOfMonthsStr) {
		int nrOfMonths;
		try {
			nrOfMonths = Integer.valueOf(nrOfMonthsStr.trim());
			if (nrOfMonths <= 0 || nrOfMonths > 36) 
				throw new RecommendationServiceException("The number of months to search for in history must be greater than zero and less than 36(3y)");
		} catch (NumberFormatException ne) {
			throw new RecommendationServiceException("The number of months to search for in history must be a number");
		}
		return nrOfMonths;
	}
	/**
	 * Private method to check if a crypto code is supported and throws exception if the check fails
	 * @param code the code to be checked
	 * @return the code trimmed and upper cased
	 */
	private String checkCryptoCode(String code) {
		code = code.trim().toUpperCase();
		if (!codeRepository.existsByCode(code))
			throw new RecommendationServiceException("The crypto code " + code + " is not supported");
		return code;
	}
}
