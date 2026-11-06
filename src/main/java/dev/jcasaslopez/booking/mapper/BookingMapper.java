package dev.jcasaslopez.booking.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.WeeklySchedule;
import dev.jcasaslopez.booking.dto.BookingRequestDto;
import dev.jcasaslopez.booking.dto.BookingResponseDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.util.ClassroomUtils;
import dev.jcasaslopez.classroom.shared.context.UserContext;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@Component
public class BookingMapper {
	
	private static final Logger logger = LoggerFactory.getLogger(BookingMapper.class);
		
	public Booking toEntity(BookingRequestDto booking, WeeklySchedule weeklySchedule) {

		int idUser = UserContext.getIdUser();

		logger.debug("Mapping BookingRequestDto to Booking: idUser={}, idClassroom={}, start={}, finish={}", 
				idUser, booking.idClassroom(), booking.start(), booking.finish());
			
		return new Booking(0,
				idUser,
				booking.idClassroom(),
				booking.start(),
				booking.finish(),
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
}