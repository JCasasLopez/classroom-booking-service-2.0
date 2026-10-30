package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.TimePointMisalignedException;
import dev.jcasaslopez.booking.exception.TimePointOutOfOpeningHoursException;

public class TimePointValidator {
	
	private static final Logger logger = LoggerFactory.getLogger(TimePointValidator.class);
	
	public static void validate(LocalDateTime timePoint, WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
		
		DayOfWeek day = timePoint.getDayOfWeek();
		DaySchedule daySchedule = weeklySchedule.scheduleFor(day);		
		DaySchedule.Open openingHours = checkDayIsOpen(day, daySchedule);
		
		checkIsWithinOpeningHours(openingHours, timePoint, slotDuration);
		
		if(!slotDuration.isAligned(timePoint.toLocalTime())) {
			 logger.debug("Time point validation failed: start time {} is not aligned to {}-minute slots",
					 timePoint.toLocalTime(), slotDuration.minutes());
			throw new TimePointMisalignedException("Time point is misaligned with the time slots");
		}
		
	}
	
	private static DaySchedule.Open checkDayIsOpen(DayOfWeek day, DaySchedule daySchedule) {
		if (!(daySchedule instanceof DaySchedule.Open open)) {
			logger.debug("Time point validation failed: business is closed on {}", day);
			throw new TimePointOutOfOpeningHoursException("Center is closed on this day");
		}
		return open;
	}
	
	private static void checkIsWithinOpeningHours(DaySchedule.Open openingHours, LocalDateTime timePoint, SlotDuration slotDuration) {
		if (timePoint.toLocalTime().isBefore(openingHours.openingTime()) 
							|| timePoint.toLocalTime().isAfter(openingHours.closingTime())) {
		    logger.debug("Time point validation failed: Time point {} is out of opening hours {} to {}", 
		    		timePoint.toLocalTime(), openingHours.openingTime(), openingHours.closingTime());
		    throw new TimePointOutOfOpeningHoursException("Time point out of opening hours");
		}
	}
}