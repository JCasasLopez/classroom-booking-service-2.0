package dev.jcasaslopez.booking.validator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;

public class RangeValidator {
		
	public static void validateTimeRange(LocalDateTime start, LocalDateTime finish, WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
		checkFinishAfterStart(start, finish);
		checkNotInPast(start, finish);
		checkStartAndFinishSameDay(start, finish);
		
		// Validate the time points are aligned with the time grid and within opening hours
		TimePointValidator.validate(start, weeklySchedule, slotDuration);		
		TimePointValidator.validate(finish, weeklySchedule, slotDuration);
	}

	public static  void validateDateRange(LocalDate start, LocalDate finish) {
		checkFinishNotBeforeStart(start, finish);
		checkNotInPast(start, finish);
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static void checkFinishAfterStart(LocalDateTime start, LocalDateTime finish) {
		if (!finish.isAfter(start)) {
			throw new IllegalArgumentException("Finish must be after start");
		}
	}
	
	private static void checkNotInPast(LocalDateTime start, LocalDateTime finish) {
		LocalDateTime now = LocalDateTime.now();
		
		if(start.isBefore(now) || finish.isBefore(now)) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}
	}
	
	private static void checkStartAndFinishSameDay(LocalDateTime start, LocalDateTime finish) {
		if (!start.toLocalDate().equals(finish.toLocalDate())) {
			throw new IllegalArgumentException("Start and finish have to be on the same day");
		}
	}
	
	private static void checkFinishNotBeforeStart(LocalDate start, LocalDate finish) {
		if (finish.isBefore(start)) {
			throw new IllegalArgumentException("Finish cannot be before start");
		}
	}
	
	private static void checkNotInPast(LocalDate start, LocalDate finish) {
		LocalDate today = LocalDate.now();
		
		if(start.isBefore(today)|| finish.isBefore(today)) {
			throw new IllegalArgumentException("Search range cannot be in the past");
		}
	}
}