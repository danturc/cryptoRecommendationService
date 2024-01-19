package crypto.recommendationService;

import org.springframework.data.repository.CrudRepository;

/**
 * Interface for database operations with codes
 */
public interface CodeRepository extends CrudRepository<Code, Integer> {
	/**
	 * Check if a specific code exists in the database
	 * @param code the crypto code
	 * @return true if the code exists, false otherwise
	 */
	public boolean existsByCode(String code);
}
