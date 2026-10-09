package dev.jcasaslopez.booking.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.SlotDuration;

public class DayScheduleParser {

	private static final Logger logger = LoggerFactory.getLogger(DayScheduleParser.class);
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

	private DayScheduleParser() {}

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
}