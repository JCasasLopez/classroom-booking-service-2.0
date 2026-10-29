package dev.jcasaslopez.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.booking.domain.SlotDuration;
import dev.jcasaslopez.booking.domain.WeeklySchedule;

@Configuration
@EnableConfigurationProperties(WeeklyScheduleProperties.class)
public class WeeklyScheduleConfig {

	@Bean
	WeeklySchedule weeklySchedule(WeeklyScheduleProperties properties, SlotDuration slot) {
		return WeeklySchedule.from(properties.hours(), slot);
	}

}
