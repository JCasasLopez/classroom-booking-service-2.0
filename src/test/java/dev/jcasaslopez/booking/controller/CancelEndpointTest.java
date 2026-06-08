package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CancelEndpointTest extends BaseIntegrationTest {
	
    @Value("${time-slot.duration}") private int slotDuration;
	
	@Test
	void cancel_endpoint_returns_the_expected_response() {
		// Arrange
		int classroomId = 1;
		BookingRequestDto bookingDto = new BookingRequestDto(1, classroomId, TestHelper.generateBookingSlots(slotDuration));
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<BookingRequestDto> httpBookingRequest = new HttpEntity<>(bookingDto, headers);
		
		// Put in a booking first, that will be cancelled in the Act section
		ResponseEntity<StandardResponse> httpBookingResponse = testRestTemplate.postForEntity(Endpoints.BOOK, 
								httpBookingRequest, StandardResponse.class);
		BookingResponseDto bookingResult = TestHelper.extractBookingResponse(httpBookingResponse.getBody(), objectMapper);
		
		// Act
		Long idBooking = bookingResult.idBooking(); 
		String cancelUrl = UriComponentsBuilder.fromPath(Endpoints.CANCEL)
				.queryParam("idBooking", idBooking)
				.toUriString();
		HttpEntity<Void> httpCancelRequest = new HttpEntity<>(headers); 
		ResponseEntity<StandardResponse> httpCancelResponse = testRestTemplate.exchange(cancelUrl, HttpMethod.PATCH,
				httpCancelRequest, StandardResponse.class);
	
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
	    headers.setBearerAuth(AuthTestHelper.generateTestJwt());
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(cancelUrl, HttpMethod.PATCH, request, StandardResponse.class);
	    
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
	    headers.setBearerAuth(AuthTestHelper.generateTestJwt());
	    HttpEntity<Void> request = new HttpEntity<>(headers);

	    // Act
	    ResponseEntity<StandardResponse> response = testRestTemplate.exchange(cancelUrl, HttpMethod.PATCH, request, StandardResponse.class);

	    // Assert
	    assertAll(
	    		() -> assertTrue(response.getBody().message().equals("Booking 9999 was not found in the database")),
	            () -> assertEquals(HttpStatus.NOT_FOUND, response.getBody().status())
	    );
	}

}