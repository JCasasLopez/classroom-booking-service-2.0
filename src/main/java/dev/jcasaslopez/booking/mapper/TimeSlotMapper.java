package dev.jcasaslopez.booking.mapper;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.dto.TimeSlotDto;

@Component
public class TimeSlotMapper {
	
	public TimeSlotDto toDto(TimeSlot timeSlot) {
		return new TimeSlotDto(timeSlot.getStart(), timeSlot.getFinish());
	}

}