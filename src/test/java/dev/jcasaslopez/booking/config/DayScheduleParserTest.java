package dev.jcasaslopez.booking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.jcasaslopez.booking.domain.DaySchedule;

public class DayScheduleParserTest {
	
	@Test
	void parse_with_valid_range_returns_correctly_parsed_open_schedule() {
	    // Act
	    DaySchedule result = DayScheduleParser.parse("9:00-11:00", DayOfWeek.SUNDAY);

	    // Assert
	    assertEquals(new DaySchedule.Open(LocalTime.of(9, 0), LocalTime.of(11, 0)), result);
	}
	
	@Test
	void parse_on_a_closed_day_returns_closed_schedule() {
	    // Act
	    DaySchedule result = DayScheduleParser.parse("CLOSED", DayOfWeek.SUNDAY);

	    // Assert
	    assertEquals(DaySchedule.CLOSED, result);
	}

	@ParameterizedTest
	@ValueSource(strings = {"9.00-11:00", "900-11:00", "9:00, 11:00", "9:0-11:00", "9:000-10:00", "closed", "CLOSEDDD", "  CLOSED"})
	void parse_with_invalid_time_format_throws_IllegalArgumentException(String rawOpeningTimes) {
		// *** See application.properties to check out valid time formats ***

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> DayScheduleParser.parse(rawOpeningTimes, DayOfWeek.SUNDAY));

	}

}
