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

import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = RecommendationServiceApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test1.properties")
public class RecommendationServiceControllerIntegrationTests1 {
	
	@Autowired
    private MockMvc mvc;
	
	@Value("${crypto.file.suffix}")
	private String cryptoFileSuffix;
	
	@Test
	public void testAllCryptosAreNotParsed() throws Exception {
		mvc.perform(get("/crypto/get_all"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
		   .andExpect(jsonPath("$", hasSize(0)));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedOtherCodes() throws Exception {
		mvc.perform(get("/crypto/get_by_code/btc"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(other codes) : BTC" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedNoData() throws Exception {
		mvc.perform(get("/crypto/get_by_code/doge"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(no data) : DOGE" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedZeroPrice() throws Exception {
		mvc.perform(get("/crypto/get_by_code/eth"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(zero or negative prices) : ETH" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedTimestamp() throws Exception {
		mvc.perform(get("/crypto/get_by_code/xrp"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(time or price format) : XRP" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedPrice() throws Exception {
		mvc.perform(get("/crypto/get_by_code/ltc"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(time or price format) : LTC" + cryptoFileSuffix));
	}
	
	@Test
	public void testCryptoNotParsedByCodeCorruptedLine() throws Exception {
		mvc.perform(get("/crypto/get_by_code/btx"))
		   .andDo(print())
		   .andExpect(status().isOk())
		   .andExpect(content().string("The crypto prices file is corrupted(insufficient data) : BTX" + cryptoFileSuffix));
	}
}
