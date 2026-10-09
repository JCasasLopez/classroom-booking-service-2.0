package dev.jcasaslopez.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.jcasaslopez.booking.domain.SlotDuration;

@Configuration
public class SlotDurationConfig {

	@Bean
	SlotDuration slotDuration(@Value("${time-slot.duration}") int minutes) {
		return new SlotDuration(minutes);
	}
}