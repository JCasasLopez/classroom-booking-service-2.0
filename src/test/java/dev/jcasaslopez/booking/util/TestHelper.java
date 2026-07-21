package dev.jcasaslopez.booking.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public final class TestHelper {
	
	private static final String email = "user@example.com";

	public static ResponseEntity<StandardResponse<BookingResponseDto>> createBooking
								(TestRestTemplate restTemplate, int idUser, int classroomId, int slotDuration) {
		BookingRequestDto bookingDto = new BookingRequestDto(idUser, classroomId, TestHelper.generateBookingSlots(slotDuration));

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(AuthTestHelper.generateTestJwt(email));
		HttpEntity<BookingRequestDto> httpBookingRequest = new HttpEntity<>(bookingDto, headers);

		return restTemplate.exchange(
				Endpoints.BOOK, 
				HttpMethod.POST, 
				httpBookingRequest, 
				new ParameterizedTypeReference<StandardResponse<BookingResponseDto>>() {} 
				);
	}
	
	// The system only allows bookings in a future date, so we look for next Monday's date
	public static List<LocalDateTime> generateBookingSlots(int slotDuration) {
		LocalTime start = LocalTime.of(10, 0);
		LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		return new ArrayList<>(List.of(
				nextMonday.atTime(start),
				nextMonday.atTime(start.plusMinutes(slotDuration))
				)
			);
	}
	
	// Start search next Monday at 10am
	public static LocalDateTime generateStartSearch(){
		LocalTime start = LocalTime.of(10, 0);
		return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atTime(start); 
	}
	
	// Finish search next Monday at 10am + periodLength
	public static LocalDateTime generateFinishSearch(int periodLength) {
		return generateStartSearch().plusMinutes(periodLength); 
	}

	public static LocalDateTime getBookingStart(List<LocalDateTime> bookingSlots) {
	    return bookingSlots.stream().min(Comparator.naturalOrder()).orElseThrow();
	}
	
	public static LocalDateTime getBookingFinish(List<LocalDateTime> bookingSlots, int slotDuration) {
		return bookingSlots.stream().max(Comparator.naturalOrder()).orElseThrow()
                .plusMinutes(slotDuration);
	}
	
	public static String findClassroomName(int classroomId, List<ClassroomEvent> classroomsStore) {
		return classroomsStore.stream()
				.filter(c -> c.getIdClassroom() == classroomId)
				.map(c -> c.getName())
				.findFirst()
			    .orElseThrow(() -> new RuntimeException("Classroom not found with id: " + classroomId));
	}

}