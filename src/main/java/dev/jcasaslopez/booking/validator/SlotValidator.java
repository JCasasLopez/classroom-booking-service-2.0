package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.OpeningHours;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.SlotNotValidException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class SlotValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(SlotValidator.class);
	
	public static void validate(LocalDateTime start, WeeklySchedule weeklySchedule, int slotDuration) {
		
		DayOfWeek day = start.getDayOfWeek();
		OpeningHours hours = weeklySchedule.getWeeklySchedule().get(day);

		if (!hours.isOpen()) {
			logger.debug("Slot validation failed: business is closed on {}", day);
			throw new SlotOutOfOpeningHoursException("Center is closed on this day");
		}

		long minutesSinceOpening = ChronoUnit.MINUTES.between(hours.openingTime(), start.toLocalTime());
		if (start.toLocalTime().isBefore(hours.openingTime())) {
			logger.debug("Slot validation failed: slot starting at {} is before opening time {}", start.toLocalTime(), hours.openingTime());
			throw new SlotOutOfOpeningHoursException("Slot starts before opening time");
		}

		// Slots are anchored to the opening time: only start times that are an exact multiple of the slot 
		// duration after opening are valid. E.g. opening at 9:00 with 15-minute slots: 9:00, 9:15, 9:30, 9:45...
		if (minutesSinceOpening % slotDuration != 0) {
			logger.debug("Slot validation failed: invalid start time {} ({}min since opening)", start, minutesSinceOpening);
			throw new SlotNotValidException("Slot does not start at a valid interval");
		}
		
		LocalTime slotEnd = start.toLocalTime().plusMinutes(slotDuration);
		if (slotEnd.isBefore(start.toLocalTime()) || slotEnd.equals(LocalTime.MIDNIGHT)) {
		    logger.debug("Slot validation failed: slot ending at {} exceeds closing time {}", slotEnd, hours.closingTime());
		    throw new SlotOutOfOpeningHoursException("Slot exceeds closing time");
		}
		if (slotEnd.isAfter(hours.closingTime())) {
		    logger.debug("Slot validation failed: slot ending at {} exceeds closing time {}", slotEnd, hours.closingTime());
		    throw new SlotOutOfOpeningHoursException("Slot exceeds closing time");
		}	
		
	}

}
