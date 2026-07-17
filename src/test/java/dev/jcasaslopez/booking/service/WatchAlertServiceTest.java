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
import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.mapper.WatchAlertMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.utility.UserContext;

@ExtendWith(MockitoExtension.class)
public class WatchAlertServiceTest {
	
	@Mock WatchAlertMapper watchAlertMapper;
	@Mock WatchAlertRepository watchAlertRepository;
	@Mock BookingRepository bookingRepository;
	@Mock EventPublisher eventPublisher;
	@InjectMocks WatchAlertServiceImpl watchAlertService;
	
	private static final long BOOKING_ID = 1L;
	private static final String EMAIL = "test@gmail.com";
	private static final int USER_ID = 1;
	private final WatchAlertRequestDto watchAlertDto = new WatchAlertRequestDto(BOOKING_ID);
	private final WatchAlert watchAlert = new WatchAlert(1L, BOOKING_ID, EMAIL);
	private final Booking bookingWithNonExistingClassroom = new Booking(BOOKING_ID, USER_ID, 9, LocalDateTime.of(2026, 5, 11, 10, 0), 
								LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 
	private final Booking bookingWithExistingClassroom = new Booking(BOOKING_ID, USER_ID, 1, LocalDateTime.of(2026, 5, 11, 10, 0), 
			LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 

	private final List<ClassroomEvent> classrooms = List.of(
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
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchBookingException.class, () -> watchAlertService.addWatchAlert(BOOKING_ID));
        verifyNoInteractions(watchAlertRepository);
    }

    @Test
    void addWatchAlert_throws_exception_if_the_classroom_is_not_found() {
		// Arrange
        ClassroomValidator realValidator = new ClassroomValidator(classrooms);
        ReflectionTestUtils.setField(watchAlertService, "classroomValidator", realValidator);

        when(watchAlertMapper.toEntity(watchAlertDto)).thenReturn(watchAlert);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(bookingWithNonExistingClassroom));

        // Act & Assert
        assertThrows(NoSuchClassroomException.class, () -> watchAlertService.addWatchAlert(BOOKING_ID));
        verifyNoInteractions(watchAlertRepository);
    }
    
    @Test
    void addWatchAlert_acts_as_expected_when_everything_is_ok() {
		// Arrange
        ClassroomValidator realValidator = new ClassroomValidator(classrooms);
        ReflectionTestUtils.setField(watchAlertService, "classroomValidator", realValidator);
        UserContext.setContext(EMAIL, USER_ID);

        when(watchAlertMapper.toEntity(watchAlertDto)).thenReturn(watchAlert);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(bookingWithExistingClassroom));
        when(watchAlertRepository.save(watchAlert)).thenReturn(watchAlert);

        // Act & Assert
        assertDoesNotThrow(() -> watchAlertService.addWatchAlert(BOOKING_ID));
        verify(watchAlertRepository).save(watchAlert);
        verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_CONFIRMED, watchAlert, UserContext.getEmail());
    }

}
