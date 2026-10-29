package dev.jcasaslopez.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Each entry is either "CLOSED" or an opening range such as "9:00-22:00"
// Example: MONDAY (open) → (9:00, 22:00), SUNDAY (closed) → CLOSED
// Midnight has to be represented as 0:00 as opposed to 24:00.

public class WeeklySchedule {
	
	private static final Logger logger = LoggerFactory.getLogger(WeeklySchedule.class);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

	private final Map<DayOfWeek, DaySchedule> scheduleByDay;

	private WeeklySchedule(Map<DayOfWeek, DaySchedule> scheduleByDay) {
	    this.scheduleByDay = Collections.unmodifiableMap(scheduleByDay);
	}

	public static WeeklySchedule from(Map<DayOfWeek, String> rawSchedule, SlotDuration timeSlot) {
		Objects.requireNonNull(rawSchedule, "rawSchedule");
		if (rawSchedule.size() != DayOfWeek.values().length) {
			throw new IllegalArgumentException("opening-times must contain exactly 7 entries, one per day of the week");
		}

		Map<DayOfWeek, DaySchedule> parsed = new EnumMap<>(DayOfWeek.class);
		rawSchedule.forEach((day, raw) -> parsed.put(day, parse(raw, day, timeSlot)));
		return new WeeklySchedule(parsed);
	}


	public Map<DayOfWeek, DaySchedule> getWeeklySchedule(){
		return scheduleByDay;
	}
	
	public static DaySchedule parse(String raw, DayOfWeek day, SlotDuration slotDuration) {
		if (raw.equals("CLOSED")) {
			logger.info("{}: CLOSED", day);
			return DaySchedule.CLOSED;
		}

		if (raw.matches("\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}")) {
			String[] parts = raw.split("-");
			LocalTime opening = LocalTime.parse(parts[0], TIME_FORMATTER);
			LocalTime closing = LocalTime.parse(parts[1], TIME_FORMATTER);

			if(!slotDuration.isAligned(opening) || !slotDuration.isAligned(closing)) {
				throw new IllegalArgumentException(
						"%s: %s must start and end on a multiple of %d minutes".formatted(day, raw, slotDuration.minutes()));
			}

			logger.info("{}: Open from {} to {}", day, opening, closing);
			return new DaySchedule.Open(opening, closing);
		}

		throw new IllegalArgumentException("Invalid opening hours format: %s".formatted(raw));
	}
	
	public DaySchedule scheduleFor(DayOfWeek dayOfWeek) {
	    return Objects.requireNonNull(scheduleByDay.get(dayOfWeek), () -> "No schedule configured for " + dayOfWeek);
	}
}