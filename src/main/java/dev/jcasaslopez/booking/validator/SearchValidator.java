package dev.jcasaslopez.booking.validator;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;

@Component
public class SearchValidator {
	
	private final WeeklySchedule weeklySchedule;
	private final SlotDuration slotDuration;
	
	public SearchValidator(WeeklySchedule weeklySchedule, SlotDuration slotDuration) {
		this.weeklySchedule = weeklySchedule;
		this.slotDuration = slotDuration;
	}

	public void validateSearch(LocalDateTime start, LocalDateTime finish) {
			
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
		
		// To validate both start and finish are within opening hours and aligned
		TimePointValidator.validate(start, weeklySchedule, slotDuration);
		TimePointValidator.validate(finish, weeklySchedule, slotDuration);

	}

}