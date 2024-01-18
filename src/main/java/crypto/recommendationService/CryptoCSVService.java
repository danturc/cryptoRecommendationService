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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

/**
 * Service Class containing the business logic for parsing data from the CSV crypto prices files
 * The class contains methods for parsing all the files from the folder, a specific crypto code or a specific day
 */
@Service
public class CryptoCSVService {
	
	/**
	 * The name of the folder containing the CSV crypto prices files
	 */
	private Path cryptoPricesFolder;
	
	/**
	 * The suffix before the code of the CSV crypto prices files
	 */
	@Value("${crypto.file.suffix}")
	private String cryptoFileSuffix;
	
	public CryptoCSVService(@Value("${crypto.prices.folder}") String cryptoPricesFolder) {
		try {
			this.cryptoPricesFolder = cryptoPricesFolder.startsWith("classpath:")?
					  				  Path.of(getClass().getResource(cryptoPricesFolder.replace("classpath:", "")).toURI()):
					  				  Path.of(cryptoPricesFolder);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			throw new RecommendationServiceException("Unknown error retrieving the crypto prices folder", e);
		}
	}
	
	/**
	 * This method parses a CSV file from the prices folder in base of a crypto code
	 * @param cryptoCode The code of the crypto to be parsed
	 * @return The crypto data containing oldest/newest/min/max prices taken from the CSV file 
	 * @throws RecommendationServiceException in case a problem is encountered during parsing, 
	 * like : file not found, corrupted data, IO error
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
	 * @throws RecommendationServiceException in case a problem is encountered during parsing, 
	 * like : file not found, corrupted data, IO error
	 */
	public CryptoData parseCryptoCSVFile(String cryptoCode, LocalDate date) {
		String fileName = cryptoCode.toUpperCase() + this.cryptoFileSuffix;
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
	        if (dataRead) 
	        	cryptoData = new CryptoData(cryptoCode.toUpperCase(), start.toLocalDateTime(), end.toLocalDateTime(), oldest, newest, min, max);
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
	 * This method parses all the crypto prices files in the CSV folder 
	 * @return a set of crypto data for all the valid files 
	 */
	public Set<CryptoData> parseCryptoCSVFolder() {
		return parseCryptoCSVFolder(null);
	}
	
	/**
	 * This method parses all the crypto prices files in the CSV folder for a specific day
	 * returning a set sorted descending in base of the normalized range of the cryptos
	 * @param date A date representing the day filter for timestamps of the lines in the prices files
	 * @return A sorted set of parsed crypto data containing oldest/newest/min/max prices taken from the CSV file
	 */
	public Set<CryptoData> parseCryptoCSVFolder(LocalDate date) {
		//init the set to be returned as a sorted TreeSet that will keep 
		//the crypto data's sorted in base of the implemented compareTo method
		Set<CryptoData> cryptos = new TreeSet<>();
		//for every crypto code found in the valid file names
		for (String code : parseCryptoCodesInFolder()) {
			try {
				//parse the corresponding CSV file
				CryptoData cryptoData = parseCryptoCSVFile(code, date);
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
	 * This method returns all the crypto codes found in the valid file names from the prices folder 
	 * @return A set of crypto codes
	 * @throws RecommendationServiceException 
	 * @throws IOException 
	 */
	public Set<String> parseCryptoCodesInFolder() {
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
	 * This method gets the crypto data with the highest normalized range in a specific day
	 * @param date The day for which to search for records in the files
	 * @return The crypto data from the crypto woth the highest normalized range in that specific day
	 * @throws RecommendationServiceException In case the date parameter is not formatted correctly
	 * or when no data is found for this specific day
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
		//thorw an exception in case there is no data parsed in the set
		if (optional.isEmpty()) 
			throw new RecommendationServiceException("There is no crypto price data for this date");
		//otherwise return the crypto data with the highest normalized range
		return optional.get();
	}
}
