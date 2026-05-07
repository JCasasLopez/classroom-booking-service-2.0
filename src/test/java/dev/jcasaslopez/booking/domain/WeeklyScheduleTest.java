package dev.jcasaslopez.booking.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

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
            "9:00-22:00", "9:00-22:00", "9:00-22:00", "9:00-22:00",
            "9:00-22:00", "10:00-20:00", "CLOSED"
        );

    	// Act
        Map<DayOfWeek, OpeningHours> schedule = new WeeklySchedule(hours).getWeeklySchedule();

        // Assert
        assertAll(
            () -> assertTrue(schedule.get(DayOfWeek.MONDAY).isOpen()),
            () -> assertTrue(schedule.get(DayOfWeek.SATURDAY).isOpen()),
            () -> assertFalse(schedule.get(DayOfWeek.SUNDAY).isOpen())
        );
    }

}
