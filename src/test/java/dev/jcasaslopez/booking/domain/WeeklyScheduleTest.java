package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class WeeklyScheduleTest {
	
	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);
	
	@Test
    void constructor_with_less_than_seven_days_throws_IllegalArgumentException() {
		Map<DayOfWeek, String> hours = Map.of(
				DayOfWeek.MONDAY, "9:00-22:00",
				DayOfWeek.TUESDAY, "9:00-22:00",
				DayOfWeek.WEDNESDAY, "9:00-22:00");

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> WeeklySchedule.from(hours, SLOT_DURATION_30));
    }

	@Test
	void from_with_empty_map_throws_IllegalArgumentException() {
		assertThrows(IllegalArgumentException.class, () -> WeeklySchedule.from(Map.of(), SLOT_DURATION_30));
	}

	@Test
	void constructor_maps_days_in_correct_order() {
		// Arrange
		Map<DayOfWeek, String> hours = Map.of(
				DayOfWeek.MONDAY, "9:00-22:00",
				DayOfWeek.TUESDAY, "CLOSED",
				DayOfWeek.WEDNESDAY, "9:00-22:00",
				DayOfWeek.THURSDAY, "9:00-22:00",
				DayOfWeek.FRIDAY, "CLOSED",
				DayOfWeek.SATURDAY, "10:00-20:00",
				DayOfWeek.SUNDAY, "CLOSED");

		// Act
		WeeklySchedule schedule = WeeklySchedule.from(hours, SLOT_DURATION_30);

		// Assert
		assertAll(
				() -> assertTrue(schedule.scheduleFor(DayOfWeek.MONDAY).isOpen()),
				() -> assertFalse(schedule.scheduleFor(DayOfWeek.TUESDAY).isOpen()),
				() -> assertTrue(schedule.scheduleFor(DayOfWeek.WEDNESDAY).isOpen()),
				() -> assertTrue(schedule.scheduleFor(DayOfWeek.THURSDAY).isOpen()),
				() -> assertFalse(schedule.scheduleFor(DayOfWeek.FRIDAY).isOpen()),
				() -> assertTrue(schedule.scheduleFor(DayOfWeek.SATURDAY).isOpen()),
				() -> assertFalse(schedule.scheduleFor(DayOfWeek.SUNDAY).isOpen())
				);
	}

}
