package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
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

	@Test
	void nextSlot_returns_the_expected_slot_when_passed_a_valid_start() {
		// Arrange
		TimeSlot timeSlot = new TimeSlot(LocalDateTime.of(2026, 4, 21, 9, 0), weeklySchedule, 30);

		// Act
		TimeSlot nextTimeSlot = timeSlot.nextSlot();

		// Assert
		assertAll(() -> assertEquals(nextTimeSlot.getStart(), (LocalDateTime.of(2026, 4, 21, 9, 30))),
				() -> assertEquals(nextTimeSlot.getFinish(), (LocalDateTime.of(2026, 4, 21, 10, 00)))
				);
	}

	@Test
	void validation_throws_SlotOutOfOpeningHoursException_when_that_day_is_closed () {
		// Act & Assert
		assertThrows(SlotOutOfOpeningHoursException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 25, 9, 0), weeklySchedule, 30));
	}
	
	@Test
	void validation_throws_SlotOutOfOpeningHoursException_when_it_is_before_opening_time () {
		// Act & Assert
		assertThrows(SlotOutOfOpeningHoursException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 21, 7, 0), weeklySchedule, 30));
	}

	@Test
	void validation_throws_SlotOutOfOpeningHoursException_when_it_is_after_closing_time () {
		// Act & Assert
		assertThrows(SlotOutOfOpeningHoursException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 21, 23, 0), weeklySchedule, 30));
	}
	
	@Test
	void validation_throws_SlotNotValidException_when_start_is_not_valid () {
		// Act & Assert
		assertThrows(SlotNotValidException.class, () -> new TimeSlot(LocalDateTime.of(2026, 4, 21, 11, 17), weeklySchedule, 30));
	}
}