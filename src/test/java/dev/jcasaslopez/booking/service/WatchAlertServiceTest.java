package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dev.jcasaslopez.booking.classroom.ClassroomValidator;
import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.event.EventPublisher;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.booking.mapper.WatchAlertMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@ExtendWith(MockitoExtension.class)
public class WatchAlertServiceTest {
	
	@Mock WatchAlertMapper watchAlertMapper;
	@Mock WatchAlertRepository watchAlertRepository;
	@Mock BookingRepository bookingRepository;
	@Mock EventPublisher eventPublisher;
	@InjectMocks WatchAlertServiceImpl watchAlertService;
	
	private long idBooking = 1L;
	private String email = "test@gmail.com";
	private WatchAlertRequestDto watchAlertDto = new WatchAlertRequestDto(idBooking);
	private WatchAlert watchAlert = new WatchAlert(1L, idBooking, email);
	private Booking bookingWithNonExistingClassroom = new Booking(idBooking, 1, 9, LocalDateTime.of(2026, 5, 11, 10, 0), 
								LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 
	private Booking bookingWithExistingClassroom = new Booking(idBooking, 1, 1, LocalDateTime.of(2026, 5, 11, 10, 0), 
			LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 

	private List<ClassroomEvent> classrooms = List.of(
		    new ClassroomEvent(1, "Main Auditorium", 150, true, true),
		    new ClassroomEvent(2, "Standard Seminar Room", 30, true, false)
		);
	
	@AfterEach
	void tearDown() {
	    UserContext.clear(); 
	}
	
	@Test
    void addWatchAlert_throws_exception_if_the_booking_is_not_found() {
		// Arrange
        when(watchAlertMapper.toEntity(watchAlertDto)).thenReturn(watchAlert);
        when(bookingRepository.findById(idBooking)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchBookingException.class, () -> watchAlertService.addWatchAlert(watchAlertDto));
        verifyNoInteractions(watchAlertRepository);
    }

    @Test
    void addWatchAlert_throws_exception_if_the_classroom_is_not_found() {
		// Arrange
        ClassroomValidator realValidator = new ClassroomValidator(classrooms);
        ReflectionTestUtils.setField(watchAlertService, "classroomValidator", realValidator);

        when(watchAlertMapper.toEntity(watchAlertDto)).thenReturn(watchAlert);
        when(bookingRepository.findById(idBooking)).thenReturn(Optional.of(bookingWithNonExistingClassroom));

        // Act & Assert
        assertThrows(NoSuchClassroomException.class, () -> watchAlertService.addWatchAlert(watchAlertDto));
        verifyNoInteractions(watchAlertRepository);
    }
    
    @Test
    void addWatchAlert_acts_as_expected_when_everything_is_ok() {
		// Arrange
        ClassroomValidator realValidator = new ClassroomValidator(classrooms);
        ReflectionTestUtils.setField(watchAlertService, "classroomValidator", realValidator);
        UserContext.setEmail(email);

        when(watchAlertMapper.toEntity(watchAlertDto)).thenReturn(watchAlert);
        when(bookingRepository.findById(idBooking)).thenReturn(Optional.of(bookingWithExistingClassroom));
        when(watchAlertRepository.save(watchAlert)).thenReturn(watchAlert);

        // Act & Assert
        assertDoesNotThrow(() -> watchAlertService.addWatchAlert(watchAlertDto));
        verify(watchAlertRepository).save(watchAlert);
        verify(eventPublisher).watchAlertEventPublisher(watchAlert, UserContext.getEmail());
    }

}
