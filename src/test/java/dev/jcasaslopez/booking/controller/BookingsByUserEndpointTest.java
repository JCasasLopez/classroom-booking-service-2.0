package dev.jcasaslopez.booking.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.exception.ClassroomNotFoundException;
import dev.jcasaslopez.booking.util.BookingEndpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.security.GenerateJwt;

// No negative idBooking test needed: @Positive validation and its handling by the GlobalExceptionHandler
// are already covered in CancelEndpointTest. No need to duplicate that coverage here.

public class BookingsByUserEndpointTest extends BaseIntegrationTest {
	
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
	@Autowired private List<ClassroomEvent> classroomsStore;
	
	@Test
	void bookings_by_user_endpoint_returns_the_expected_response() {
		// Arrange
		TestHelper.createBooking(testRestTemplate, USER_ID, CLASSROOM_ID, slotDuration.minutes());
		
		// Act
		ResponseEntity<StandardResponse<List<BookingResponseDto>>> httpResponse = testRestTemplate.exchange(
				createURL(), 
				HttpMethod.GET,
				new HttpEntity<>(createHeader()), 
				new ParameterizedTypeReference<StandardResponse<List<BookingResponseDto>>>() {});		
		
		// Assert
		List<BookingResponseDto> bookings = httpResponse.getBody().details();	

		assertAll(
				() -> assertEquals(HttpStatus.OK, getResponseHttpStatus(httpResponse)),
				() -> assertEquals(1, bookings.size()),
				() -> assertEquals(getExpectedClassroomName(), bookings.get(0).classroomName()),
				() -> assertTrue(getHttpResponseBody(httpResponse).message().equals(String.format("Bookings by user %s retrieved successfully", USER_ID)))
				);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private LocalDateTime nextMondayAt9() {
		return LocalDate.now()
		        .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
		        .atTime(9, 0);
	}
	
	private LocalDateTime nextMondayAt12() {
		return LocalDate.now()
		        .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
		        .atTime(12, 0);
	}
	
	private HttpHeaders createHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(new GenerateJwt(secretKey).withIdUser(USER_ID).build());
		return headers;
	}
	
	private String createURL() {
		return UriComponentsBuilder.fromPath(BookingEndpoints.BOOKINGS_BY_USER)
				.queryParam("idUser", USER_ID)
				.queryParam("startSearch", nextMondayAt9())
				.queryParam("finishSearch", nextMondayAt12())
				.toUriString();
	}
	
	private String getExpectedClassroomName() {
		return classroomsStore.stream()
			    .filter(c -> c.idClassroom() == CLASSROOM_ID)
			    .findAny()
			    .orElseThrow(() -> new ClassroomNotFoundException("Classroom not found")).name();
	}
	
	private StandardResponse<List<BookingResponseDto>> getHttpResponseBody(ResponseEntity<StandardResponse<List<BookingResponseDto>>> httpResponse) {
		return httpResponse.getBody();
	}
	
	private HttpStatus getResponseHttpStatus(ResponseEntity<StandardResponse<List<BookingResponseDto>>> httpResponse) {
		return httpResponse.getBody().status();
	}

}
