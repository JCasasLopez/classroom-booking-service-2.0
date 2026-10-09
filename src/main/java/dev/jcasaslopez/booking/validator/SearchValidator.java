package dev.jcasaslopez.booking.validator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.DaySchedule;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.exception.SlotOutOfOpeningHoursException;

@Component
public class SearchValidator {
	
	private final WeeklySchedule weeklySchedule;
	
	public SearchValidator(WeeklySchedule weeklySchedule) {
		this.weeklySchedule = weeklySchedule;
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

		DayOfWeek searchDayOfWeek = start.getDayOfWeek();
		DaySchedule daySchedule = weeklySchedule.scheduleFor(searchDayOfWeek);

		if (!(daySchedule instanceof DaySchedule.Open open)) {
			throw new SlotOutOfOpeningHoursException("The center is closed that day");
		}

		if (start.toLocalTime().isBefore(open.openingTime()) ||
				finish.toLocalTime().isAfter(open.closingTime())) {
			throw new SlotOutOfOpeningHoursException("Start or finish out of opening hours");
		}		

	}

}
