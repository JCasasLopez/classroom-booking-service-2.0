package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

// No negative idUser test needed: @Positive validation and its handling by the GlobalExceptionHandler
// are already covered in CancelEndpointTest. No need to duplicate that coverage here.

public class BookingsByUserEndpointTest extends BaseIntegrationTest {
	
	private static final int SLOT_DURATION = 30;
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	private static final String email = "user@example.com";
	
	@Test
	void bookings_by_user_endpoint_returns_the_expected_response() {
		// Arrange
		TestHelper.createBooking(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);
		
		// Act
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt(email));
		String userBookingsUrl = UriComponentsBuilder.fromPath(Endpoints.USER_BOOKINGS)
				.queryParam("idUser", USER_ID)
				.toUriString();
		HttpEntity<Void> httpRequest = new HttpEntity<>(headers); 
		ResponseEntity<StandardResponse<List<BookingResponseDto>>> httpResponse = testRestTemplate.exchange(
				userBookingsUrl, 
				HttpMethod.GET,
				httpRequest, 
				new ParameterizedTypeReference<StandardResponse<List<BookingResponseDto>>>() {});
		
		// Assert
		StandardResponse<List<BookingResponseDto>> responseBody = httpResponse.getBody();
		List<BookingResponseDto> bookings = httpResponse.getBody().details();
		
		assertAll(
				() -> assertEquals(HttpStatus.OK, responseBody.status()),
				() -> assertNotNull(bookings),
				() -> assertTrue(responseBody.message().equals(String.format("Bookings by user %s retrieved successfully", USER_ID)))
				);
	}

}
