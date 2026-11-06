package dev.jcasaslopez.booking.validator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;

@Component
public class RangeValidator {
	
	public static void validateRange(LocalDateTime start, LocalDateTime finish, WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
			
		if (!finish.isAfter(start)) {
			throw new IllegalArgumentException("Finish must be after start");
		}

		LocalDateTime now = LocalDateTime.now();
		if(start.isBefore(now) || finish.isBefore(now)) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}

		if (!start.toLocalDate().equals(finish.toLocalDate())) {
			throw new IllegalArgumentException("Start and finish have to be in the same day");
		}
	
		// Validate the time points are aligned with the time grid and within opening hours
		TimePointValidator.validate(start, weeklySchedule, slotDuration);		
		TimePointValidator.validate(finish, weeklySchedule, slotDuration);
	}

	public static  void validateCalendarAvailabilityDates(LocalDate start, LocalDate finish) {
		if (start.isAfter(finish)) {
			throw new IllegalArgumentException("Start date cannot be after finish date");
		}

		LocalDate today = LocalDate.now();
		if(start.isBefore(today)) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}

	}

}