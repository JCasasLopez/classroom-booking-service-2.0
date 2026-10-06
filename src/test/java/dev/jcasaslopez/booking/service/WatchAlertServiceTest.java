package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.dto.WatchAlertRequestDto;
import dev.jcasaslopez.booking.dto.WatchAlertResponseDto;
import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.mapper.WatchAlertMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.context.UserContext;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;

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
		
	private final WatchAlertResponseDto watchAlertResponseDto = new WatchAlertResponseDto(
	        "Main Auditorium",
	        LocalDateTime.of(2026, 5, 11, 10, 0),
	        LocalDateTime.of(2026, 5, 11, 11, 0));
	
	private final WatchAlert watchAlert = new WatchAlert(1L, BOOKING_ID, EMAIL);
	
	private static final Booking ACTIVE_BOOKING = new Booking(BOOKING_ID, USER_ID, 1, LocalDateTime.of(2026, 5, 11, 10, 0), 
			LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.ACTIVE); 
	
	private static final Booking NON_ACTIVE_BOOKING = new Booking(BOOKING_ID, USER_ID, 1, LocalDateTime.of(2026, 5, 11, 10, 0), 
			LocalDateTime.of(2026, 5, 11, 11, 0), LocalDateTime.now(), BookingStatus.CANCELLED); 
	
	@AfterEach
	void tearDown() {
	    UserContext.clear(); 
	}
	
	@Test
	void addWatchAlert_throws_exception_if_the_booking_is_not_found() {
	    // Arrange
	    when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

	    // Act & Assert
	    assertThrows(BookingNotFoundExceptions.class, () -> watchAlertService.addWatchAlert(BOOKING_ID, EMAIL));
	    verifyNoInteractions(watchAlertRepository);
	    verifyNoInteractions(eventPublisher);
	}

	@Test
	void addWatchAlert_throws_exception_if_the_booking_is_not_active() {
	    // Arrange
	    when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(NON_ACTIVE_BOOKING));

	    // Act & Assert
	    assertThrows(IllegalStateException.class, () -> watchAlertService.addWatchAlert(BOOKING_ID, EMAIL));
	    verifyNoInteractions(watchAlertRepository);
	    verifyNoInteractions(eventPublisher);
	}

	@Test
	void addWatchAlert_acts_as_expected_when_everything_is_ok() {
	    // Arrange
	    when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(ACTIVE_BOOKING));
	    when(watchAlertMapper.toEntity(any(WatchAlertRequestDto.class))).thenReturn(watchAlert);
	    when(watchAlertRepository.save(watchAlert)).thenReturn(watchAlert);
	    when(watchAlertMapper.toResponseDto(eq(watchAlert), any(), eq(bookingRepository)))
	            .thenReturn(watchAlertResponseDto); 

	    // Act & Assert
	    assertDoesNotThrow(() -> watchAlertService.addWatchAlert(BOOKING_ID, EMAIL));
	    verify(watchAlertRepository).save(watchAlert);
	    verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_CONFIRMED, watchAlert, EMAIL);
	}
	
	@Test
	void watchAlertsList_throws_exception_if_start_is_after_finish() {
	    // Arrange
	    LocalDateTime start = LocalDateTime.of(2026, 5, 11, 11, 0);
	    LocalDateTime finish = LocalDateTime.of(2026, 5, 11, 10, 0);

	    // Act & Assert
	    assertThrows(IllegalArgumentException.class,
	            () -> watchAlertService.watchAlertsListByUserAndTimePeriod(start, finish, EMAIL));
	    verifyNoInteractions(watchAlertRepository);
	}

	@Test
	void watchAlertsList_throws_exception_if_start_equals_finish() {
	    // Arrange
	    LocalDateTime sameInstant = LocalDateTime.of(2026, 5, 11, 10, 0);

	    // Act & Assert
	    assertThrows(IllegalArgumentException.class,
	            () -> watchAlertService.watchAlertsListByUserAndTimePeriod(sameInstant, sameInstant, EMAIL));
	    verifyNoInteractions(watchAlertRepository);
	}

}