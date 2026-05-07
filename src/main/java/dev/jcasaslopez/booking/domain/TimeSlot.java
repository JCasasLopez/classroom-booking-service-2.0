package dev.jcasaslopez.booking.domain;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.exception.SlotNotValidException;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

public class TimeSlot implements Comparable<TimeSlot>{
	
	private static final Logger logger = LoggerFactory.getLogger(TimeSlot.class);

	private final LocalDateTime start;
	private final LocalDateTime finish;
	private final int slotDuration;
	private final WeeklySchedule weeklySchedule;
	
	public TimeSlot(LocalDateTime start, WeeklySchedule weeklySchedule, int slotDuration) {		
		if (start == null) throw new IllegalArgumentException("The start of a TimeSlot cannot be null");
		this.start = start;
		this.slotDuration = slotDuration;
		this.weeklySchedule = weeklySchedule;
		
		validateSlot(start);
		
		// Built after validation of 'start'
		this.finish = start.plusMinutes(slotDuration);
		logger.debug("TimeSlot created: start={}, finish={}", this.start, this.finish);
	}
	

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getFinish() {
		return finish;
	}

	public TimeSlot nextSlot () {
		LocalDateTime nextSlotStart = this.getStart().plusMinutes(slotDuration);
		return new TimeSlot(nextSlotStart, weeklySchedule, slotDuration);
	}

	private void validateSlot(LocalDateTime start) {
		DayOfWeek day = start.getDayOfWeek();
		OpeningHours hours = weeklySchedule.getWeeklySchedule().get(day);

		if (!hours.isOpen()) {
			logger.warn("Slot validation failed: business is closed on {}", day);
			throw new SlotOutOfOpeningHoursException("Center is closed on this day");
		}

		long minutesSinceOpening = ChronoUnit.MINUTES.between(hours.openingTime(), start.toLocalTime());
		if (start.toLocalTime().isBefore(hours.openingTime())) {
			logger.warn("Slot validation failed: slot starting at {} is before opening time {}", start.toLocalTime(), hours.openingTime());
			throw new SlotOutOfOpeningHoursException("Slot starts before opening time");
		}

		// Let us say slot duration is 15 minutes, then, only slots starting at :00, :15, :30 and :45 would be valid.
		if (minutesSinceOpening % slotDuration != 0) {
			logger.warn("Slot validation failed: invalid start time {} ({}min since opening)", start, minutesSinceOpening);
			throw new SlotNotValidException("Slot does not start at a valid interval");
		}
		
		LocalTime slotEnd = start.toLocalTime().plusMinutes(slotDuration);
		if (slotEnd.isBefore(start.toLocalTime()) || slotEnd.equals(LocalTime.MIDNIGHT)) {
		    logger.warn("Slot validation failed: slot ending at {} exceeds closing time {}", slotEnd, hours.closingTime());
		    throw new SlotOutOfOpeningHoursException("Slot exceeds closing time");
		}
		if (slotEnd.isAfter(hours.closingTime())) {
		    logger.warn("Slot validation failed: slot ending at {} exceeds closing time {}", slotEnd, hours.closingTime());
		    throw new SlotOutOfOpeningHoursException("Slot exceeds closing time");
		}	
	}

	@Override
	public int compareTo(TimeSlot anotherTimeSlot) {
		return this.start.compareTo(anotherTimeSlot.start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		TimeSlot slot = (TimeSlot) obj;
		return Objects.equals(start, slot.start);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start);
	}

	@Override
	public String toString() {
		return "Start: " + start + " Finish: " + finish;
	}

}