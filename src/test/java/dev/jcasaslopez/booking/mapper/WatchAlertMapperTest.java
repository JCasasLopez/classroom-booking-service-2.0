package dev.jcasaslopez.booking.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.util.ClassroomUtils;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@ExtendWith(MockitoExtension.class)
public class WatchAlertMapperTest {
	
	@Mock List<ClassroomEvent> classroomsStore;
	@Mock BookingRepository bookingRepository;
	
	// We instantiate WatchAlertMapper by hand instead so we do not need to use Spring and the whole context
	private final static WatchAlertMapper mapper = new WatchAlertMapper();

	@Test
	void watchAlertMapper_maps_correctly_to_ResponseDto() {
		// Arrange
		String userEmail = "test@gmail.com";
		Long idBooking = 236L;
		String classroomName = "101";
		LocalDateTime start = LocalDateTime.of(2026, 11, 26, 9, 0);
		LocalDateTime finish = LocalDateTime.of(2026, 11, 26, 9, 30);

		Booking booking = new Booking(idBooking, 3, 12, start, finish, LocalDateTime.now(), BookingStatus.ACTIVE);
		WatchAlert watchAlert = new WatchAlert(0L, idBooking, userEmail);
		
		when(bookingRepository.findById(idBooking)).thenReturn(Optional.of(booking));
		try (MockedStatic<ClassroomUtils> mockedStatic = Mockito.mockStatic(ClassroomUtils.class)) {
			mockedStatic.when(() -> ClassroomUtils.findClassroomName(booking, classroomsStore))
			.thenReturn(classroomName);
			
		// Act
		WatchAlertResponseDto watchAlertResponseDto = mapper.toResponseDto(watchAlert, classroomsStore, bookingRepository);

		// Assert
		assertAll(
				() -> assertEquals(classroomName, watchAlertResponseDto.classroomName()),
				() -> assertEquals(start, watchAlertResponseDto.start()),
				() -> assertEquals(finish, watchAlertResponseDto.finish())
				);
		}

	}
	
	@Test
	void watchAlertMapper_throws_NoSuchBookingException_if_does_not_find_booking() {
		// Arrange
		WatchAlert watchAlert = new WatchAlert(0L, 236L, "test@gmail.com");
	    when(bookingRepository.findById(236L)).thenReturn(Optional.empty());
		
		// Act & Assert
	    assertThrows(NoSuchBookingException.class, () -> mapper.toResponseDto(watchAlert, classroomsStore, bookingRepository));
	}

}
