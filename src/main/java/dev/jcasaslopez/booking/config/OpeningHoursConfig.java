package dev.jcasaslopez.booking.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import jakarta.annotation.PostConstruct;

@Configuration
public class OpeningHoursConfig {

	@Value("${opening-times.monday}") private String mondayHours; 
	@Value("${opening-times.tuesday}") private String tuesdayHours;
	@Value("${opening-times.wednesday}") private String wednesdayHours;
	@Value("${opening-times.thursday}") private String thursdayHours;
	@Value("${opening-times.friday}") private String fridayHours;
	@Value("${opening-times.saturday}") private String saturdayHours; 
	@Value("${opening-times.sunday}") private String sundayHours;

	private List<String> weeklyHours;

    @PostConstruct
	private void init() {
		// It is initialized here because the @Value values are assigned after dependency injection.
		weeklyHours = List.of(mondayHours, tuesdayHours, wednesdayHours, thursdayHours, fridayHours, saturdayHours, sundayHours);
	}

    // WeeklySchedule holds the opening schedule for a whole week: one DaySchedule per DayOfWeek.
    // Each entry is either "CLOSED" or an opening range such as "9:00-22:00" (see DayScheduleParser for the accepted format).
    // Example: MONDAY (open) → (9:00, 22:00), SUNDAY (closed) → CLOSED
    @Bean
    WeeklySchedule weeklySchedule(SlotDuration slotDuration) {
    	return new WeeklySchedule(weeklyHours, slotDuration);
    }

}
