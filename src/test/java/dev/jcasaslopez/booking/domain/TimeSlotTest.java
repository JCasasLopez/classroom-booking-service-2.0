package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.jcasaslopez.booking.exception.SlotNotValidException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class TimeSlotTest {
	
	private final WeeklySchedule weeklySchedule = 
			new WeeklySchedule(List.of("9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "CLOSED", "CLOSED"));

	
	@Test
	void null_start_throws_IllegalArgumentException() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> new TimeSlot(null, weeklySchedule, 30));
	}

	// Verify open slots (including edge cases) do not throw exception (via instatiation) and nextSlot() works as expected.
	@ParameterizedTest
	@MethodSource("openSlots")
	void nextSlot_returns_the_expected_slot_when_passed_a_valid_start(LocalDateTime slotStart, LocalDateTime nextSlotStart, 
			LocalDateTime nextSlotFinish) {
		// Arrange
		TimeSlot timeSlot = new TimeSlot(slotStart, weeklySchedule, 30);

		// Act
		TimeSlot nextTimeSlot = timeSlot.nextSlot();

		// Assert
		assertAll(() -> assertEquals(nextTimeSlot.getStart(), nextSlotStart),
				() -> assertEquals(nextTimeSlot.getFinish(), nextSlotFinish)
				);
	}
	
	private static Stream<Arguments> openSlots() {
		return Stream.of(
				// On opening time
				Arguments.of(LocalDateTime.of(2026, 4, 21, 9, 0), LocalDateTime.of(2026, 4, 21, 9, 30), LocalDateTime.of(2026, 4, 21, 10, 0)),	
				// After opening time
				Arguments.of(LocalDateTime.of(2026, 4, 21, 11, 0), LocalDateTime.of(2026, 4, 21, 11, 30), LocalDateTime.of(2026, 4, 21, 12, 0)),
				// Just before closing time
				Arguments.of(LocalDateTime.of(2026, 4, 21, 20, 0), LocalDateTime.of(2026, 4, 21, 20, 30), LocalDateTime.of(2026, 4, 21, 21, 0))				
				);
	}

	// Verify the exception message introduces a certain degree of coupling with the implementation, but it is necessary 
	// to make sure the cause of the exception is the right one. 
	@ParameterizedTest
	@MethodSource("closedSlots")
	void validation_fails_when_slots_is_out_of_opening_hours (LocalDateTime start, String exceptionMessage) {
		// Arrange

		// Act & Assert
		SlotOutOfOpeningHoursException ex = assertThrows(SlotOutOfOpeningHoursException.class, () -> new TimeSlot(start, weeklySchedule, 30));
		assertTrue(ex.getMessage().equals(exceptionMessage));
	}
	
	private static Stream<Arguments> closedSlots() {
		return Stream.of(
				// Saturday
				Arguments.of(LocalDateTime.of(2026, 5, 9, 11, 0),  "Center is closed on this day"),  
				// Sunday
				Arguments.of(LocalDateTime.of(2026, 5, 10, 11, 0), "Center is closed on this day"),  
				// Open day, before opening time 
				Arguments.of(LocalDateTime.of(2026, 5, 7, 7, 0),   "Slot starts before opening time"),	
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
		assertThrows(SlotNotValidException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 21, 11, 17), weeklySchedule, 30));
	}

}	