package dev.jcasaslopez.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.jcasaslopez.booking.domain.OpeningHours;

class OpeningHourTest {

	@Test
	void openingHours_with_null_start_throws_IllegalArgumentException() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> new OpeningHours (null, LocalTime.of(9, 0)));
		
	}
	
	@Test
	void openingHours_with_null_finish_throws_IllegalArgumentException() {
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> new OpeningHours (LocalTime.of(9, 0), null));
		
	}
	
	@Test
	void a_day_where_it_is_closed_parses_OpeningHours_values_to_null() {
		// Act
		OpeningHours openingHoursOnCloseDay = OpeningHours.parse("CLOSED", DayOfWeek.SUNDAY);

		// Assert
		assertAll(() -> assertNull(openingHoursOnCloseDay.openingTime()),
				() -> assertNull(openingHoursOnCloseDay.closingTime()));
	}
	
	@Test
	void parse_method_with_valid_parameters_returns_correct_LocalTime() {
		// Act
		OpeningHours openingHoursOnOpenDay = OpeningHours.parse("9:00-11:00", DayOfWeek.SUNDAY);

		// Assert
		assertAll(
			    () -> assertEquals(openingHoursOnOpenDay.openingTime(), LocalTime.of(9, 0)),
			    () -> assertEquals(openingHoursOnOpenDay.closingTime(), LocalTime.of(11, 0))
			);
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"9.00-11:00", "900-11:00", "9:00, 11:00", "9:0-11:00", "9:000-10:00", "closed", "CLOSEDDD", "  CLOSED"})
	void openingHours_with_invalid_time_format_throws_IllegalArgumentException(String rawOpeningTimes) {
		// *** See application.properties to check out valid time formats ***
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> OpeningHours.parse(rawOpeningTimes, DayOfWeek.SUNDAY));

	}
	
}