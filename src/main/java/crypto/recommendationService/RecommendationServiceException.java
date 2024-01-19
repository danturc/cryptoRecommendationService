package crypto.recommendationService;

/**
 * Custom exception class for the crypto recommendation service
 */
public class RecommendationServiceException extends RuntimeException {

	private static final long serialVersionUID = -9210935407855329477L;
	
	public RecommendationServiceException(String message) {
		super(message);
	}
	
	public RecommendationServiceException(String message, Throwable t) {
		super(message, t);
	}
}
