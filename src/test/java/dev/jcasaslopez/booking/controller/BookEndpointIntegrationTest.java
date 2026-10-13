package dev.jcasaslopez.booking.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.base.BaseIntegrationTest;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.util.TestHelper;
import dev.jcasaslopez.classroom.shared.dto.StandardResponse;

public class BookEndpointIntegrationTest extends BaseIntegrationTest {
	
	private static final int CLASSROOM_ID = 1;
	private static final int USER_ID = 1;
    
    @Test
	void book_endpoint_returns_created_status_and_expected_booking_details() {
		// Arrange
		String classroomName = TestHelper.findClassroomName(CLASSROOM_ID, classroomsStore);

		List<LocalDateTime> bookingSlots = TestHelper.generateBookingSlots(slotDuration.minutes());	
		LocalDateTime bookingStart = TestHelper.getBookingStart(bookingSlots);
		LocalDateTime bookingFinish = TestHelper.getBookingFinish(bookingSlots, slotDuration.minutes());
		
		// Act
		ResponseEntity<StandardResponse<BookingResponseDto>> httpResponse = TestHelper.createBooking
													(testRestTemplate, USER_ID, CLASSROOM_ID, slotDuration.minutes());
		
		// Assert
		BookingResponseDto bookingResult = httpResponse.getBody().details();
		String finalMessage = String.format("Classroom %s booked successfully", CLASSROOM_ID);

		assertAll(
				() -> assertEquals(HttpStatus.CREATED, httpResponse.getBody().status()),
				() -> assertEquals(finalMessage, httpResponse.getBody().message()),
				() -> assertEquals(classroomName, bookingResult.classroomName()),
				() -> assertEquals(BookingStatus.ACTIVE, bookingResult.status()),
				() -> assertEquals(bookingStart, bookingResult.start()),
				() -> assertEquals(bookingFinish, bookingResult.finish())
				);
	}

}