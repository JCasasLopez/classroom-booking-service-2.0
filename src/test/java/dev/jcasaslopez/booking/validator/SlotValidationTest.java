package dev.jcasaslopez.booking.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.SlotNotValidException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class SlotValidationTest {
	
	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	
	private final WeeklySchedule weeklySchedule = 
			new WeeklySchedule(List.of("9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "CLOSED", "CLOSED"),
					SLOT_DURATION_30);
	
	@ParameterizedTest
	@MethodSource("openSlots")
	void happy_path(LocalDateTime start) {
		// Arrange

		// Act & Assert
		assertDoesNotThrow(() -> new TimeSlot(start, weeklySchedule, SLOT_DURATION_30));
	}

	private static Stream<Arguments> openSlots(){
		return Stream.of(
				// Monday, opening time
				Arguments.of(LocalDateTime.of(2026, 5, 4, 9, 0)),  
				// Monday, halfway through the day
				Arguments.of(LocalDateTime.of(2026, 5, 4, 11, 0)),  
				// Monday, just before closing time
				Arguments.of(LocalDateTime.of(2026, 5, 4, 20, 30))		
				);
	}
	
	// Verify the exception message introduces a certain degree of coupling with the implementation, but it is necessary 
	// to make sure the cause of the exception is the right one. 
	@ParameterizedTest
	@MethodSource("closedSlots")
	void validation_fails_when_slots_is_out_of_opening_hours (LocalDateTime start, String exceptionMessage) {
		// Arrange

		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, () -> new TimeSlot(start, weeklySchedule, SLOT_DURATION_30));
		assertEquals(exceptionMessage, ex.getMessage());
	}

	private static Stream<Arguments> closedSlots() {
		return Stream.of(
				// Saturday
				Arguments.of(LocalDateTime.of(2026, 5, 9, 11, 0),  "Center is closed on this day"),  
				// Sunday
				Arguments.of(LocalDateTime.of(2026, 5, 10, 11, 0), "Center is closed on this day"),  
				// Open day, before opening time 
				Arguments.of(LocalDateTime.of(2026, 5, 7, 7, 0),   "Slot starts before opening time"),	
				// Open day, just before opening time
				Arguments.of(LocalDateTime.of(2026, 5, 7, 8, 30),  "Slot starts before opening time"),
				// Open day, on closing time 
				Arguments.of(LocalDateTime.of(2026, 5, 7, 21, 0),  "Slot exceeds closing time"),
				// Open day, after closing time 
				Arguments.of(LocalDateTime.of(2026, 5, 7, 23, 0),  "Slot exceeds closing time"),
				// Open day, just before next day
				Arguments.of(LocalDateTime.of(2026, 5, 7, 23, 30), "Slot exceeds closing time")
				);
	}

	@Test
	void validation_throws_SlotNotValidException_when_start_is_not_valid () {
		// Act & Assert
		assertThrows(SlotNotValidException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 21, 11, 17), weeklySchedule, SLOT_DURATION_30));
	}


}
