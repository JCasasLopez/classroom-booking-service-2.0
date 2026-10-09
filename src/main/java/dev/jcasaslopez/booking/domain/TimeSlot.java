package dev.jcasaslopez.booking.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.validator.SlotValidator;

public class TimeSlot implements Comparable<TimeSlot>{
	
	private static final Logger logger = LoggerFactory.getLogger(TimeSlot.class);

	private final LocalDateTime start;
	private final LocalDateTime finish;
	private final SlotDuration slotDuration;
	private final WeeklySchedule weeklySchedule;
	
	public TimeSlot(LocalDateTime start, WeeklySchedule weeklySchedule, SlotDuration slotDuration) {		
		if (start == null) throw new IllegalArgumentException("The start of a TimeSlot cannot be null");
		this.start = start;
		this.slotDuration = slotDuration;
		this.weeklySchedule = weeklySchedule;
		
		SlotValidator.validate(start, weeklySchedule, slotDuration);
		
		// Built after validation of 'start'
		this.finish = start.plusMinutes(slotDuration.minutes());
		logger.debug("TimeSlot created: start={}, finish={}", this.start, this.finish);
	}
	

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getFinish() {
		return finish;
	}

	public TimeSlot nextSlot () {
		LocalDateTime nextSlotStart = this.getStart().plusMinutes(slotDuration.minutes());
		return new TimeSlot(nextSlotStart, weeklySchedule, slotDuration);
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