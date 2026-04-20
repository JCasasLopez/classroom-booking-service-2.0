package dev.jcasaslopez.booking.domain;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// WeeklySchedule class is a Map that has (see the class definition in domain package for details):
// - As keys: Java's DayOfWeek enum values.
// - As values: OpeningHours object as defined in the domain package.
// Example:  MONDAY (open)   → (9:00, 22:00)
//           SUNDAY (closed) → (null, null)

public class WeeklySchedule {

	private Map<DayOfWeek, OpeningHours> weeklySchedule;

	public WeeklySchedule(List<String> weeklyHours) {
		this.weeklySchedule = addOpeningHours(weeklyHours);
	}

	public Map<DayOfWeek, OpeningHours> getWeeklySchedule() {
		return weeklySchedule;
	}

	private Map<DayOfWeek, OpeningHours> addOpeningHours(List<String> weeklyHours) {
	    DayOfWeek[] days = DayOfWeek.values();
	    return IntStream.range(0, weeklyHours.size())
	        .boxed()
	        .collect(Collectors.toMap(
	            i -> days[i],
	            i -> OpeningHours.parse(weeklyHours.get(i), days[i])
	        ));
	}

}
