package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.List;

import org.junit.jupiter.api.Test;

public class WeeklyScheduleTest {
	
	@Test
    void constructor_with_less_than_seven_days_throws_IllegalArgumentException() {
		// Arrange
        List<String> hours = List.of("9:00-22:00", "9:00-22:00", "9:00-22:00");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new WeeklySchedule(hours));
    }

    @Test
    void constructor_with_more_than_seven_days_throws_IllegalArgumentException() {
		// Arrange
    	List<String> hours = List.of(
            "9:00-22:00", "9:00-22:00", "9:00-22:00", "9:00-22:00",
            "9:00-22:00", "9:00-22:00", "9:00-22:00", "9:00-22:00"
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new WeeklySchedule(hours));
    }

    @Test
    void constructor_maps_days_in_correct_order() {
		// Arrange
    	List<String> hours = List.of(
            "9:00-22:00", "CLOSED", "9:00-22:00", "9:00-22:00",
            "CLOSED", "10:00-20:00", "CLOSED"
        );

    	// Act
    	WeeklySchedule schedule = new WeeklySchedule(hours);

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
