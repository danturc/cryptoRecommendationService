package crypto.recommendationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = RecommendationServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class RecommendationServiceControllerIntegrationTests {
	
	@Autowired
    private MockMvc mvc;
	
	@Value("${crypto.file.suffix}")
	private String cryptoFileSuffix;
	
	@Test
	public void testAllCryptosAreParsed() throws Exception {
		mvc.perform(get("/crypto/get/all"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$", hasSize(5)))
		   .andExpect(jsonPath("$[0].code", is("ETH")));
	}
	
	@Test
	public void testCryptoIsParsedByCode() throws Exception {
		double oldest = 46813.21;
		double newest = 38415.79;
		double min = 33276.59;
		double max = 47722.66;
		mvc.perform(get("/crypto/get/code/btc"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$.code", is("BTC")))
		   .andExpect(jsonPath("$.oldest").value(is(oldest), Double.class))
		   .andExpect(jsonPath("$.newest").value(is(newest), Double.class))
		   .andExpect(jsonPath("$.min").value(is(min), Double.class))
		   .andExpect(jsonPath("$.max").value(is(max), Double.class))
		   .andExpect(jsonPath("$.normalizedRange").value(is((max - min) / min), Double.class));
	}
	
	@Test
	public void testCryptoIsParsedByDate() throws Exception {
		double oldest = 0.8298;
		double newest = 0.8458;
		double min = 0.8298;
		double max = 0.8458;
		mvc.perform(get("/crypto/get/date/01-01-2022"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$.code", is("XRP")))
		   .andExpect(jsonPath("$.startTime", startsWith(LocalDate.of(2022, 1, 1).toString())))
		   .andExpect(jsonPath("$.endTime", startsWith(LocalDate.of(2022, 1, 1).toString())))
		   .andExpect(jsonPath("$.oldest").value(is(oldest), Double.class))
		   .andExpect(jsonPath("$.newest").value(is(newest), Double.class))
		   .andExpect(jsonPath("$.min").value(is(min), Double.class))
		   .andExpect(jsonPath("$.max").value(is(max), Double.class))
		   .andExpect(jsonPath("$.normalizedRange").value(is((max - min) / min), Double.class));
	}
	
	@Test
	public void testCryptoNotParsedByCodeThatNotExists() throws Exception {
		mvc.perform(get("/crypto/get/code/btx"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto code BTX is not supported"));
	}
	
	@Test
	public void testCryptoNotParsedByCodeThatHasNoFile() throws Exception {
		mvc.perform(get("/crypto/add/code/btz"));
		mvc.perform(get("/crypto/get/code/btz"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file cannot be found : BTZ" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByDateWithWrongFormat() throws Exception {
		mvc.perform(get("/crypto/get/date/01.01.2022"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("Incorrect format for date parameter"));
	}
	
	@Test
	public void testCryptoNotParsedByDateWithNoData() throws Exception {
		mvc.perform(get("/crypto/get/date/01-01-2023"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("There is no crypto price data for this date"));
	}
	
	@Test
	public void testGetAllCryptoCodes() throws Exception {
		mvc.perform(get("/crypto/get/codes"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$", hasSize(5)));
	}
	
	@Test
	public void testAddNewCryptoCodeAlreadyExists() throws Exception {
		mvc.perform(get("/crypto/add/code/btc"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string("The crypto code BTC already exists"));
	}
	
	@Test
	public void testAddNewCryptoCode() throws Exception {
		mvc.perform(get("/crypto/add/code/bty"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$.code", is("BTY")));
	}
	
	@Test
	public void testAddNewCryptoCodeTooLong() throws Exception {
		mvc.perform(get("/crypto/add/code/btcxxx"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string("The crypto code cannot have more than 5 characters"));
		mvc.perform(get("/crypto/get/codes"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$", hasSize(5)));
	}
	
	@Test
	public void testAddNewCryptoCodeNotValid() throws Exception {
		mvc.perform(get("/crypto/add/code/btc1"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().string("The crypto code must contain only alphabetic characters"));
		mvc.perform(get("/crypto/get/codes"))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		.andExpect(jsonPath("$", hasSize(5)));
	}
	
	@Test
	public void testAllCryptosAreGetFromHistory() throws Exception {
		mvc.perform(get("/crypto/get/all"));
		mvc.perform(get("/crypto/get/history/all/36"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$", hasSize(5)))
		   .andExpect(jsonPath("$[0].code", is("ETH")));
	}
	
	@Test
	public void testAllCryptosAreNotGetFromHistoryWhenNoData() throws Exception {
		mvc.perform(get("/crypto/get/all"));
		mvc.perform(get("/crypto/get/history/all/12"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("There is no crypto data  in the last 12 months"));
	}
	
	@Test
	public void testAllCryptosAreNotGetFromHistoryWhenMonthTooBig() throws Exception {
		mvc.perform(get("/crypto/get/history/all/40"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The number of months to search for in history must be greater than zero and less than 36(3y)"));
	}
	
	@Test
	public void testAllCryptosAreNotGetFromHistoryWhenMonthNotValid() throws Exception {
		mvc.perform(get("/crypto/get/history/all/xx"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The number of months to search for in history must be a number"));
	}
	
	@Test
	public void testCryptoIsGetByCodeFromHistory() throws Exception {
		mvc.perform(get("/crypto/get/code/btc"));
		double oldest = 46813.21;
		double newest = 38415.79;
		double min = 33276.59;
		double max = 47722.66;
		mvc.perform(get("/crypto/get/history/code/btc/36"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$.code", is("BTC")))
		   .andExpect(jsonPath("$.oldest").value(is(oldest), Double.class))
		   .andExpect(jsonPath("$.newest").value(is(newest), Double.class))
		   .andExpect(jsonPath("$.min").value(is(min), Double.class))
		   .andExpect(jsonPath("$.max").value(is(max), Double.class))
		   .andExpect(jsonPath("$.normalizedRange").value(is((max - min) / min), Double.class));
	}
	
	@Test
	public void testCryptoIsNotGetByCodeFromHistoryWhenNoData() throws Exception {
		mvc.perform(get("/crypto/get/code/btc"));
		mvc.perform(get("/crypto/get/history/code/btc/12"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("There is no data for the crypto BTC in the last 12 months"));
	}
	
	@Test
	public void testCryptosIsNotGetByCodeFromHistoryWhenMonthTooBig() throws Exception {
		mvc.perform(get("/crypto/get/history/all/40"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The number of months to search for in history must be greater than zero and less than 36(3y)"));
	}
	
	@Test
	public void testCryptosIsNotGetByCodeFromHistoryWhenMonthNotValid() throws Exception {
		mvc.perform(get("/crypto/get/history/all/xx"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The number of months to search for in history must be a number"));
	}
}
