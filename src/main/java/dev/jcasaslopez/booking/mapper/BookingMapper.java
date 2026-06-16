package dev.jcasaslopez.booking.mapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.TimeSlot;
import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.util.ClassroomUtils;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Component
public class BookingMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(BookingMapper.class);
	
	@Value("${time-slot.duration}") private final int slotDuration; 
	
	public BookingMapper(@Value("${time-slot.duration}") int slotDuration) {
		this.slotDuration = slotDuration;
	}
	
	public Booking toEntity(BookingRequestDto booking, WeeklySchedule weeklySchedule) {
		logger.debug("Mapping BookingRequestDto to Booking: idUser={}, idClassroom={}, slots={}", 
		        booking.idUser(), booking.idClassroom(), booking.startTimeSlotList().size());
		List<TimeSlot> timeSlots = convertStartToTimeSlots(booking, weeklySchedule);
		return new Booking(0,
							booking.idUser(),
							booking.idClassroom(),
							timeSlots.get(0).getStart(),
							timeSlots.get(timeSlots.size()-1).getFinish(),
							LocalDateTime.now(),
							BookingStatus.ACTIVE);		
	}
	
	public BookingResponseDto toResponseDto (Booking booking, List<ClassroomEvent> classroomsStore) {
		logger.debug("Mapping Booking to BookingResponseDto: idBooking={}", booking.getIdBooking());
		return new BookingResponseDto(booking.getIdBooking(),
				ClassroomUtils.findClassroomName(booking, classroomsStore), 
				booking.getStart(),
				booking.getFinish(),
				booking.getStatus());
	}
	
	private List<TimeSlot> convertStartToTimeSlots (BookingRequestDto booking, WeeklySchedule weeklySchedule) {
		Collections.sort(booking.startTimeSlotList());
		
		// By converting the start times of every slot into an TimeSlot object, we are validating that the booking times 
		// are within opening hours, etc. (see TimeSlot for validations).
		return booking.startTimeSlotList().stream()
				.map(b -> new TimeSlot(b, weeklySchedule, slotDuration))
				.toList();
	}

}