package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.SlotNotValidException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class SlotValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(SlotValidator.class);
	
	public static void validate(LocalDateTime start, WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
		
		int slotDurationInMinutes = slotDuration.minutes();
		DayOfWeek day = start.getDayOfWeek();
		DaySchedule daySchedule = weeklySchedule.scheduleFor(day);

		if (!(daySchedule instanceof DaySchedule.Open open)) {
			logger.debug("Slot validation failed: business is closed on {}", day);
			throw new SlotOutOfOpeningHoursException("Center is closed on this day");
		}
		
		if (start.toLocalTime().isBefore(open.openingTime())) {
		    logger.debug("Slot validation failed: slot starting at {} is before opening time {}",start.toLocalTime(), open.openingTime());
		    throw new SlotOutOfOpeningHoursException("Slot starts before opening time");
		}
		
		long minutesSinceOpening = ChronoUnit.MINUTES.between(open.openingTime(), start.toLocalTime());

		// Slots are anchored to the opening time: only start times that are an exact multiple of the slot 
		// duration after opening are valid. E.g. opening at 9:00 with 15-minute slots: 9:00, 9:30...
		if (minutesSinceOpening % slotDurationInMinutes != 0) {
			logger.debug("Slot validation failed: invalid start time {} ({}min since opening)", start, minutesSinceOpening);
			throw new SlotNotValidException("Slot does not start at a valid interval");
		}
		
		LocalDateTime slotEnd = start.plusMinutes(slotDurationInMinutes);
		LocalDateTime closing = start.toLocalDate().atTime(open.closingTime());

		if (slotEnd.isAfter(closing)) {
		    logger.debug("Slot validation failed: slot ending at {} exceeds closing time {}",
		            slotEnd.toLocalTime(), open.closingTime());
		    throw new SlotOutOfOpeningHoursException("Slot exceeds closing time");
		}	
	}
}