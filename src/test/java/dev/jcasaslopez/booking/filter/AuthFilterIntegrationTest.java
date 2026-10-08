package dev.jcasaslopez.booking.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.util.BookingEndpoints;

//Even though the authentication filter has already been tested comprehensively through unit tests (both in this 
//micro-service and in the shared library), a proper integration test to verify the wiring is a good safety net.

//Choosing a fail scenario (invalid token) has two advantages: there is no need to create a valid token, and,
//most importantly, there is no need to spin up the database with Testcontainers, as there is no interaction
//with it, speeding up the test.

public class AuthFilterIntegrationTest extends BaseIntegrationTest {
	
	@Autowired TestRestTemplate testRestTemplate; 
	private static final String invalidToken = "Invalid token";
	
	@Test
	void auth_filter_returns_401_when_token_is_invalid() {
		// Arrange
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(invalidToken);
		
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		
		// Act
		ResponseEntity<String> response = testRestTemplate.exchange(
				BookingEndpoints.BOOKINGS,
		        HttpMethod.GET,
		        requestEntity,
		        String.class
		    );
		
		// Assert
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());		
	}
}