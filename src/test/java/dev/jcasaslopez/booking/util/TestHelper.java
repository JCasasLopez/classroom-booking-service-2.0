package dev.jcasaslopez.booking.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public final class TestHelper {
	
	@Autowired
	static List<ClassroomEvent> classroomsStore;
		
	// The system only allows bookings in a future date, so we look for next Monday's date
	public static List<LocalDateTime> generateBookingSlots(int slotDuration) {
		LocalTime start = LocalTime.of(10, 0);
		LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		return List.of(
				nextMonday.atTime(start),
				nextMonday.atTime(start.plusMinutes(slotDuration))
			);
	}
	
	public static LocalDateTime getBookingStart(List<LocalDateTime> bookingSlots) {
	    return bookingSlots.stream().min(Comparator.naturalOrder()).orElseThrow();
	}
	
	public static LocalDateTime getBookingFinish(List<LocalDateTime> bookingSlots, int slotDuration) {
		return bookingSlots.stream().max(Comparator.naturalOrder()).orElseThrow()
                .plusMinutes(slotDuration);
	}
	
	public static String findClassroomName(int classroomId) {
		return classroomsStore.stream()
				.filter(c -> c.getIdClassroom() == classroomId)
				.map(c -> c.getName())
				.findFirst()
			    .orElseThrow(() -> new RuntimeException("Classroom not found with id: " + classroomId));
	}
	
	public static BookingResponseDto extractBookingResponse(StandardResponse body, ObjectMapper mapper) {
	    return mapper.convertValue(body.details(), BookingResponseDto.class);
	}

}