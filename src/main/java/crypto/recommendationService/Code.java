package crypto.recommendationService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bean class representing the crypto code
 * The class is also an entity mapped to the codes table
 */
@Entity
@Table(name="codes")
public class Code {
	/**
	 * The autogenerated Id
	 */
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;
	
	/**
	 * The crypto code
	 */
	@Column(name="code")
	private String code;
	
	/**
	 * Default empty constructor
	 */
	public Code() {}
	
	/**
	 * Constructor with the code
	 * @param code the crypto code
	 */
	public Code(String code) {
		this.code = code;
	}
	
	/*
	 * Getters and setters
	 */

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	@Override
	public String toString() {
		return this.code;
	}
}