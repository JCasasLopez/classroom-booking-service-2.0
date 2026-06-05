package dev.jcasaslopez.booking.controller;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.testHelper.TestHelper;
import dev.jcasaslopez.booking.util.Endpoints;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

public class BookEndpointTest extends BaseIntegrationTest {

    @Value("${time-slot.duration}") private int slotDuration;
    
    @Test
	void book_endpoint_returns_the_expected_response() {
		// Arrange
		LocalTime start = LocalTime.of(10, 0);
		int classroomId = 1;
		
		LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		List<LocalDateTime> startTimeSlotList = List.of(
				nextMonday.atTime(start),
				nextMonday.atTime(start.plusMinutes(slotDuration))
			);
		
		LocalDateTime bookingStart = nextMonday.atTime(start);
		LocalDateTime bookingFinish = nextMonday.atTime(start.plusMinutes(slotDuration * startTimeSlotList.size()));
		
		String classroomName = classroomStore.stream()
				.filter(c -> c.getIdClassroom() == classroomId)
				.map(c -> c.getName())
				.findFirst()
			    .orElseThrow(() -> new RuntimeException("Classroom not found with id: " + classroomId));
		
		BookingRequestDto bookingRequest = new BookingRequestDto (1, classroomId, startTimeSlotList);
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(TestHelper.generateTestJwt());
		HttpEntity<BookingRequestDto> request = new HttpEntity<>(bookingRequest, headers);
		
		// Act
		ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Endpoints.BOOK, request, StandardResponse.class);
		BookingResponseDto bookingResponse = TestHelper.extractBookingResponse(response.getBody(), objectMapper);

		// Assert
		assertAll(
				() -> assertEquals(HttpStatus.CREATED, response.getStatusCode()),
				() -> assertEquals(classroomName, bookingResponse.name()),
				() -> assertEquals(BookingStatus.ACTIVE, bookingResponse.status()),
				() -> assertEquals(bookingStart, bookingResponse.start()),
				() -> assertEquals(bookingFinish, bookingResponse.finish())
				);
	}

    @Test
    void book_endpoint_returns_400_when_body_is_missing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestHelper.generateTestJwt());
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
        headers.setBearerAuth(TestHelper.generateTestJwt());
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<StandardResponse> response = testRestTemplate.postForEntity(Endpoints.BOOK, request, StandardResponse.class);

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
            () -> assertEquals("Validation failed for one or more fields",response.getBody().message())
        );
    }
}