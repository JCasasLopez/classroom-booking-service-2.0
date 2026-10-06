package dev.jcasaslopez.booking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.exception.BookingNotFoundExceptions;
import dev.jcasaslopez.booking.exception.InvalidBookingStatusException;
import dev.jcasaslopez.booking.kafka.event.EventPublisher;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.WatchAlertRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;

// No unit tests for BookingService methods other than cancel():

// book(): its observable effects (persisted entity, emitted event) are covered by the controller integration test
// and KafkaProducerIntegrationTest, respectively.
// A unit test would only verify the EventPublisher call, at the cost of heavy stubbing: low ROI.

// bookingsByUser(), markBookingsAsCompleted(): no business logic; the queries are covered by the repository tests.

@ExtendWith(MockitoExtension.class)
public class BookingServiceUnitTest {
	
	@Mock BookingRepository bookingRepository;
	@Mock WatchAlertRepository watchAlertRepository;
	@Mock EventPublisher eventPublisher;

	@InjectMocks private BookingServiceImpl bookingService;
	
	private static final long ID_BOOKING = 1;
	private static final int ID_CLASSROOM = 1;
	private static final int ID_USER = 1;
	private static final String EMAIL = "test@gmail.com";
	private static final String OTHER_EMAIL = "other_test@gmail.com";
	private static final String THIRD_EMAIL = "third@gmail.com";

	private static final LocalDateTime START = LocalDateTime.of(2026, 4, 21, 9, 0);
	private static final LocalDateTime FINISH = LocalDateTime.of(2026, 4, 21, 9, 30);
	
	private static final WatchAlert WATCH_ALERT = new WatchAlert(1, ID_BOOKING, OTHER_EMAIL);
	private static final WatchAlert WATCH_ALERT_2 = new WatchAlert(2, ID_BOOKING, THIRD_EMAIL);
	
	@Test
	void cancel_publishes_cancellation_event() {
		// Arrange
		Booking booking = activeBookingToCancel();

		// Act 
		bookingService.cancel(ID_BOOKING, ID_USER, EMAIL);

		// Assert
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.BOOKING_CANCELLED, booking, EMAIL);
	}

	@Test
	void cancel_triggers_one_watch_alert_per_watcher() {
		// Arrange
		Booking booking = activeBookingToCancel();
		when(watchAlertRepository.findWatchAlertsByBooking(ID_BOOKING)).thenReturn(List.of(WATCH_ALERT, WATCH_ALERT_2));

		// Act 
		bookingService.cancel(ID_BOOKING, ID_USER, EMAIL);

		// Assert
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_TRIGGERED, booking, OTHER_EMAIL);
		verify(eventPublisher).publishBookingRelatedEvent(NotificationType.WATCH_ALERT_TRIGGERED, booking, THIRD_EMAIL);
	}

	@Test
	void if_there_is_no_such_booking_throws_exception() {
		// Arrange
		when(bookingRepository.findById(ID_BOOKING)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(BookingNotFoundExceptions.class, () -> bookingService.cancel(ID_BOOKING, ID_USER, EMAIL));
	}

	@Test
	void if_booking_does_not_belong_to_identified_user_throws_exception() {
		// Arrange
		Booking otherUsersBooking = new Booking(ID_BOOKING, 
				ID_USER + 1, 
				ID_CLASSROOM,
				START, 
				FINISH, 
				LocalDateTime.now(), 
				BookingStatus.ACTIVE);

		when(bookingRepository.findById(ID_BOOKING)).thenReturn(Optional.of(otherUsersBooking));

		// Act & Assert
		assertThrows(BookingNotFoundExceptions.class, () -> bookingService.cancel(ID_BOOKING, ID_USER, EMAIL));
	}

	@Test
	void if_booking_is_not_active_throws_exception() {
		// Arrange
		Booking completeBooking = new Booking(ID_BOOKING, 
				ID_USER, 
				ID_CLASSROOM,
				START, 
				FINISH, 
				LocalDateTime.now(), 
				BookingStatus.COMPLETED);

		when(bookingRepository.findById(ID_BOOKING)).thenReturn(Optional.of(completeBooking));

		// Act & Assert
		assertThrows(InvalidBookingStatusException.class, () -> bookingService.cancel(ID_BOOKING, ID_USER, EMAIL));
	}

	private Booking activeBookingToCancel() {
		Booking booking = new Booking(ID_BOOKING, ID_USER, ID_CLASSROOM,
				START, FINISH, LocalDateTime.now(), BookingStatus.ACTIVE);
		when(bookingRepository.findById(ID_BOOKING)).thenReturn(Optional.of(booking));
		return booking;
	}

}
