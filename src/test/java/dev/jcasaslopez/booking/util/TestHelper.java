package dev.jcasaslopez.booking.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.StandardResponse;

public final class TestHelper {
	
	// The system only allows bookings in a future date, so we look for next Monday's date
	public static List<LocalDateTime> generateBookingSlots(int slotDuration) {
		LocalTime start = LocalTime.of(10, 0);
		LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
		return List.of(
				nextMonday.atTime(start),
				nextMonday.atTime(start.plusMinutes(slotDuration))
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
	
	public static BookingResponseDto extractBookingResponse(StandardResponse body, ObjectMapper mapper) {
	    return mapper.convertValue(body.details(), BookingResponseDto.class);
	}
	
	public static List<BookingResponseDto> extractBookingList(StandardResponse body, ObjectMapper mapper) {
	    return mapper.convertValue(body.details(), new TypeReference<List<BookingResponseDto>>() {});
	}
	
	// SlotStatusDto contains a TimeSlot, whose constructor calls validateSlot(), which requires
	// WeeklySchedule to be Spring-injected. Since Jackson instantiates objects via reflection,
	// outside the Spring context, weeklySchedule is null and deserialization fails.
	// Deserializing to List<?> (LinkedHashMap internally) avoids constructing TimeSlot altogether
	public static List<?> extractSlotStatusList(StandardResponse body, ObjectMapper mapper) {
		return mapper.convertValue(body.details(), new TypeReference<List<?>>() {});
	}

}