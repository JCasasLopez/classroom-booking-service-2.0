package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public class CancelEndpointTest extends BaseIntegrationTest {
	
	private static final int SLOT_DURATION = 30;
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	private static final String email = "user@example.com";
	
	@Test
	void cancel_endpoint_returns_the_expected_response() {
		// Arrange
		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = TestHelper.createBooking
													(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);
		
		// Act
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt(email));
		Long idBooking = httpResponse.getBody().details().idBooking(); 
		String cancelUrl = UriComponentsBuilder.fromPath(Endpoints.CANCEL)
				.queryParam("idBooking", idBooking)
				.toUriString();
		HttpEntity<Void> httpCancelRequest = new HttpEntity<>(headers); 

		ResponseEntity<StandardResponse<Void>> httpCancelResponse = testRestTemplate.exchange(
				cancelUrl, 
				HttpMethod.PATCH,
				httpCancelRequest, 
				new ParameterizedTypeReference<StandardResponse<Void>>() {});
	
		// Assert
		Booking savedBooking = repository.findById(idBooking)
			    					.orElseThrow(() -> new NoSuchBookingException("Booking not found in the database"));
		assertAll(
				() -> assertEquals(HttpStatus.OK, httpCancelResponse.getBody().status()),
				() -> assertEquals(BookingStatus.CANCELLED, savedBooking.getStatus())
				);
	}
	
	@Test
	void cancel_endpoint_returns_400_when_idBooking_is_invalid() {
	    // Arrange
	    String cancelUrl = UriComponentsBuilder.fromPath(Endpoints.CANCEL)
	            .queryParam("idBooking", -1)
	            .toUriString();
	    HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(AuthTestHelper.generateTestJwt(email));
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse<String>> response = testRestTemplate.exchange(
	    		cancelUrl, 
	    		HttpMethod.PATCH, 
	    		request, 
	    		new ParameterizedTypeReference<StandardResponse<String>>() {}
	    	);
	    
	    // Assert
	    assertAll(
	            () -> assertEquals(HttpStatus.BAD_REQUEST, response.getBody().status())
	    );
	}
	
	@Test
	void cancel_endpoint_returns_404_when_booking_not_found() {
	    // Arrange
	    String cancelUrl = UriComponentsBuilder.fromPath(Endpoints.CANCEL)
	            .queryParam("idBooking", 9999)
	            .toUriString();
	    HttpHeaders headers = new HttpHeaders();
	    headers.setBearerAuth(AuthTestHelper.generateTestJwt(email));
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse<String>> response = testRestTemplate.exchange(
	    		cancelUrl, 
	    		HttpMethod.PATCH, 
	    		request, 
	    		new ParameterizedTypeReference<StandardResponse<String>>() {}
	    	);
	    
	    // Assert
	    assertAll(
	    		() -> assertTrue(response.getBody().message().equals("Booking 9999 was not found in the database")),
	            () -> assertEquals(HttpStatus.NOT_FOUND, response.getBody().status())
	    );
	}

}