package dev.jcasaslopez.booking.domain;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.jcasaslopez.booking.config.DayScheduleParser;

// WeeklySchedule holds the opening schedule for a whole week: one DaySchedule per DayOfWeek.
// Each entry is either "CLOSED" or an opening range such as "9:00-22:00" (see DayScheduleParser for the accepted format).
// Example: MONDAY (open) → (9:00, 22:00), SUNDAY (closed) → CLOSED

public class WeeklySchedule {

	private final Map<DayOfWeek, DaySchedule> weeklySchedule;

	public WeeklySchedule(List<String> weeklyHours, SlotDuration slotDuration) {
		if (weeklyHours.size() != DayOfWeek.values().length) {
			throw new IllegalArgumentException("weeklyHours must contain exactly 7 entries, one per day of the week");
		}
		this.weeklySchedule = addOpeningHours(weeklyHours, slotDuration);
	}
	
	public Map<DayOfWeek, DaySchedule> getWeeklySchedule(){
		return weeklySchedule;
	}

	public DaySchedule scheduleFor(DayOfWeek dayOfWeek) {
	    return Objects.requireNonNull(weeklySchedule.get(dayOfWeek), () -> "No schedule configured for " + dayOfWeek);
	}
	
	private Map<DayOfWeek, DaySchedule> addOpeningHours(List<String> weeklyHours, SlotDuration slotDuration) {
	    DayOfWeek[] days = DayOfWeek.values();
	    return IntStream.range(0, weeklyHours.size())
	        .boxed()
	        .collect(Collectors.toMap(
	            i -> days[i],
	            i -> DayScheduleParser.parse(weeklyHours.get(i), days[i], slotDuration)
	        ));
	}

}
