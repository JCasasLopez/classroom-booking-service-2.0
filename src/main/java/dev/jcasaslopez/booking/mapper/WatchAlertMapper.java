package dev.jcasaslopez.booking.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.util.ClassroomUtils;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@Component
public class WatchAlertMapper {
		
	public WatchAlert toEntity (WatchAlertRequestDto watchAlertRequestDto) {
		String userEmail = UserContext.getEmail();
		return new WatchAlert (0, watchAlertRequestDto.idBooking(), userEmail);
	}
	
	public WatchAlertResponseDto toResponseDto (WatchAlert watchAlert, List<ClassroomEvent> classroomsStore, 
			BookingRepository bookingRepository) {
		 Booking booking = bookingRepository.findById(watchAlert.getIdBooking())
		            .orElseThrow(() -> new NoSuchBookingException("No booking with that id was found in the database"));

		    String classroomName = ClassroomUtils.findClassroomName(booking, classroomsStore);
		    return new WatchAlertResponseDto(classroomName, booking.getStart(), booking.getFinish());
	}
}