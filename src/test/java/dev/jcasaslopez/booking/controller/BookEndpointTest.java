package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public class BookEndpointTest extends BaseIntegrationTest {

	private static final int SLOT_DURATION = 30;
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
    
    @Test
	void book_endpoint_returns_the_expected_response() {
		// Arrange
		String classroomName = TestHelper.findClassroomName(CLASSROOM_ID, classroomsStore);

		List<LocalDateTime> bookingSlots = TestHelper.generateBookingSlots(SLOT_DURATION);	
		LocalDateTime bookingStart = TestHelper.getBookingStart(bookingSlots);
		LocalDateTime bookingFinish = TestHelper.getBookingFinish(bookingSlots, SLOT_DURATION);
		
		// Act
		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = TestHelper.createBooking
													(testRestTemplate, USER_ID, CLASSROOM_ID, SLOT_DURATION);
		
		// Assert
		BookingResponseDto bookingResult = httpResponse.getBody().details();

		assertAll(
				() -> assertEquals(HttpStatus.CREATED, httpResponse.getBody().status()),
				() -> assertEquals(classroomName, bookingResult.classroomName()),
				() -> assertEquals(BookingStatus.ACTIVE, bookingResult.status()),
				() -> assertEquals(bookingStart, bookingResult.start()),
				() -> assertEquals(bookingFinish, bookingResult.finish())
				);
	}

    @Test
    void book_endpoint_returns_400_when_body_is_missing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(AuthTestHelper.generateTestJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<StandardResponse<String>> httpResponse = testRestTemplate.exchange(
		        Endpoints.BOOK, 
		        HttpMethod.POST, 
		        request, 
		        new ParameterizedTypeReference<StandardResponse<String>>() {} 
		);
        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, httpResponse.getStatusCode()),
            () -> assertEquals("Malformed or unreadable JSON request", httpResponse.getBody().message())
        );
    }

    @Test
    void book_endpoint_returns_400_when_required_fields_are_null() {
        String body = """
            {
              "idUser": null,
              "idClassroom": null,
              "startTimeSlotList": null
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(AuthTestHelper.generateTestJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<StandardResponse<String>> httpResponse = testRestTemplate.exchange(
		        Endpoints.BOOK, 
		        HttpMethod.POST, 
		        request, 
		        new ParameterizedTypeReference<StandardResponse<String>>() {} 
		);
        
        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, httpResponse.getStatusCode()),
            () -> assertEquals("Validation failed for one or more fields", httpResponse.getBody().message())
        );
    }
}