package dev.jcasaslopez.booking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;

public class WeeklyScheduleParsingTest {

	private static final SlotDuration SLOT_DURATION_30 = new SlotDuration(30);

	// from() requires all 7 days, so we fill the rest with CLOSED and set only the day under test
	private static WeeklySchedule weekWith(DayOfWeek day, String raw) {
		Map<DayOfWeek, String> rawWeek = new EnumMap<>(DayOfWeek.class);
		for (DayOfWeek d : DayOfWeek.values()) {
			rawWeek.put(d, "CLOSED");
		}
		rawWeek.put(day, raw);
		return WeeklySchedule.from(rawWeek, SLOT_DURATION_30);
	}

	@Test
	void from_with_valid_range_returns_correctly_parsed_open_schedule() {
		WeeklySchedule schedule = weekWith(DayOfWeek.SUNDAY, "9:00-11:00");

		assertEquals(new DaySchedule.Open(LocalTime.of(9, 0), LocalTime.of(11, 0)),
				schedule.scheduleFor(DayOfWeek.SUNDAY));
	}

	@Test
	void from_on_a_closed_day_returns_closed_schedule() {
		WeeklySchedule schedule = weekWith(DayOfWeek.SUNDAY, "CLOSED");

		assertEquals(DaySchedule.CLOSED, schedule.scheduleFor(DayOfWeek.SUNDAY));
	}

	@ParameterizedTest
	@ValueSource(strings = {"9.00-11:00", "900-11:00", "9:00, 11:00", "9:0-11:00", "9:000-10:00", "closed", "CLOSEDDD", "  CLOSED"})
	void from_with_invalid_time_format_throws_IllegalArgumentException(String rawOpeningTimes) {
		// *** See application.properties to check out valid time formats ***
		assertThrows(IllegalArgumentException.class, () -> weekWith(DayOfWeek.SUNDAY, rawOpeningTimes));
	}

}