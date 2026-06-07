package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.util.AuthTestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public class BookEndpointTest extends BaseIntegrationTest {

    @Value("${time-slot.duration}") private int slotDuration;
    
    @Test
	void book_endpoint_returns_the_expected_response() {
		// Arrange
		int classroomId = 1;
		String classroomName = TestHelper.findClassroomName(classroomId);

		List<LocalDateTime> bookingSlots = TestHelper.generateBookingSlots(slotDuration);	
		LocalDateTime bookingStart = TestHelper.getBookingStart(bookingSlots);
		LocalDateTime bookingFinish = TestHelper.getBookingFinish(bookingSlots, slotDuration);
		
		BookingRequestDto bookingDto = new BookingRequestDto(1, classroomId, bookingSlots);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt());
		HttpEntity<BookingRequestDto> httpRequest = new HttpEntity<>(bookingDto, headers);
		
		// Act
		ResponseEntity<StandardResponse> httpResponse = testRestTemplate.postForEntity(Endpoints.BOOK, httpRequest, StandardResponse.class);

		// Assert
		BookingResponseDto bookingResult = TestHelper.extractBookingResponse(httpResponse .getBody(), objectMapper);
		assertAll(
				() -> assertEquals(HttpStatus.CREATED, httpResponse.getStatusCode()),
				() -> assertEquals(classroomName, bookingResult.name()),
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
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Endpoints.BOOK, request, StandardResponse.class);

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
            () -> assertEquals("Malformed or unreadable JSON request",response.getBody().message())
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

        ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Endpoints.BOOK, request, StandardResponse.class);

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
            () -> assertEquals("Validation failed for one or more fields",response.getBody().message())
        );
    }
}