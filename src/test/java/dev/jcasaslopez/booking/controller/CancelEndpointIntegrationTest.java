package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import dev.jcasaslopez.booking.exception.BookingNotFoundException;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;
import dev.jcasaslopez.classroom.shared.security.GenerateJwt;

public class CancelEndpointIntegrationTest extends BaseIntegrationTest {
	
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	
	@Test
	void cancel_endpoint_returns_ok_status_and_expected_booking_status_update() {
		// Arrange
		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = TestHelper.createBooking
													(testRestTemplate, USER_ID, CLASSROOM_ID, slotDuration.minutes());
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(new GenerateJwt(secretKey).withIdUser(USER_ID).build());
		Long idBooking = httpResponse.getBody().details().idBooking(); 
		
		String cancelUrl = UriComponentsBuilder.fromPath(BookingEndpoints.CANCEL)
		        .buildAndExpand(idBooking)
		        .toUriString();
				
		HttpEntity<Void> httpCancelRequest = new HttpEntity<>(headers); 

		// Act
		ResponseEntity<StandardResponse<Void>> httpCancelResponse = testRestTemplate.exchange(
				cancelUrl, 
				HttpMethod.PATCH,
				httpCancelRequest, 
				new ParameterizedTypeReference<StandardResponse<Void>>() {});
	
		// Assert
		Booking savedBooking = repository.findById(idBooking)
								.orElseThrow(() -> new BookingNotFoundException("Booking not found in the database"));
								
		assertAll(
				() -> assertEquals(HttpStatus.OK, httpCancelResponse.getBody().status()),
				() -> assertEquals("Booking cancelled successfully", httpCancelResponse.getBody().message()),
				() -> assertEquals(BookingStatus.CANCELLED, savedBooking.getStatus())
		);
	}

}