package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
		assertAll(() -> assertEquals(nextSlotStart, nextTimeSlot.getStart()),
				() -> assertEquals(nextSlotFinish, nextTimeSlot.getFinish())
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
}	