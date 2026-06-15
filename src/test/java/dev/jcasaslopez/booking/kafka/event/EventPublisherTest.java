package dev.jcasaslopez.booking.kafka.event;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.kafka.producer.NotificationEventProducer;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@ExtendWith(MockitoExtension.class)
public class EventPublisherTest {
	
	@Mock NotificationEventProducer notificationEventProducer;
	@Mock BookingRepository bookingRepository;

	private EventPublisherImpl eventPublisher;
	
	private static final String EMAIL = "test@gmail.com";
	
	private static final Booking BOOKING = new Booking(0, 1, 1,
			LocalDateTime.of(2026, 5, 11, 11, 0),
			LocalDateTime.of(2026, 5, 11, 12, 0),
			LocalDateTime.now(),
			BookingStatus.ACTIVE);
	
	private static final WatchAlert WATCH_ALERT = new WatchAlert(1L, BOOKING.getIdBooking(), EMAIL);
	
	private static final List<ClassroomEvent> allClassrooms = List.of(
		    new ClassroomEvent(1, "Main Auditorium", 150, true, true),
		    new ClassroomEvent(2, "Standard Seminar Room", 30, true, false)
		);
	
	@BeforeEach
	void setUp() {
	    eventPublisher = new EventPublisherImpl(
	        allClassrooms,
	        notificationEventProducer,
	        bookingRepository
	    );
	}
	
	@ParameterizedTest
	@MethodSource("NotificationFieldsProvider")
	void publishBookingRelatedEvent_sends_correct_message_when_notification_type_is_the_right_type
							(String expectedSubject, String expectedLog, String expectedMessage, NotificationType type) {
		// Arrange
		
		// Act
		eventPublisher.publishBookingRelatedEvent(type, BOOKING, EMAIL);
		
		// Assert
		ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
		verify(notificationEventProducer).sendNotification(captor.capture());
		NotificationEvent notification = captor.getValue();
		assertAll(
				() -> assertEquals(expectedSubject, notification.subject()),
				() -> assertEquals(expectedLog, notification.log()),
				// Contains instead of equals because the message is wrapped in HTML tags
				() -> assertTrue(notification.message().contains(expectedMessage))
				);
		
	}
	
	// Each argument: (expected subject, expected log, expected message fragment, notification type)
	static Stream<Arguments> NotificationFieldsProvider() {
		return Stream.of(
				bookingArgs(NotificationType.BOOKING_CONFIRMED,
						"We are pleased to confirm your booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00"),
				bookingArgs(NotificationType.BOOKING_CANCELLED,
						"We are pleased to confirm that your booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00 has been succesfully cancelled"),
				bookingArgs(NotificationType.WATCH_ALERT_TRIGGERED,
						"A booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00 has been cancelled. Hurry up and book it before someone else does!")
				);
	}

	// Builds test arguments deriving subject and log from the enum to avoid hardcoding them
	private static Arguments bookingArgs(NotificationType type, String expectedMessage) {
		return Arguments.of(
				type.getSubject(),
				String.format(type.getLogText(), EMAIL),
				expectedMessage,
				type
				);
	}
	
	@Test
	void publishBookingRelatedEvent_throws_exception_when_notification_type_is_the_wrong_type_for_bookings() {
		// Arrange
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, 
				() -> eventPublisher.publishBookingRelatedEvent(NotificationType.WATCH_ALERT_CONFIRMED, BOOKING, EMAIL));
		
	}
	
	// Tests the second overloaded method
	@Test
	void publishBookingRelatedEvent_sends_correct_message_when_notification_type_is_watch_alert_confirmed() {
		// Arrange
		when(bookingRepository.findById(WATCH_ALERT.getIdBooking())).thenReturn(Optional.of(BOOKING));
		String expectedSubject = NotificationType.WATCH_ALERT_CONFIRMED.getSubject();
		String expectedLog = String.format(NotificationType.WATCH_ALERT_CONFIRMED.getLogText(), EMAIL);
		String expectedMessage = "We are pleased to confirm your watch alert for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00. If the booking is cancelled you will be notified.";
	
		// Act
		eventPublisher.publishBookingRelatedEvent(NotificationType.WATCH_ALERT_CONFIRMED, WATCH_ALERT, EMAIL);
		
		// Assert
		ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
		verify(notificationEventProducer).sendNotification(captor.capture());
		NotificationEvent notification = captor.getValue();
		assertAll(
				() -> assertEquals(expectedSubject, notification.subject()),
				() -> assertEquals(expectedLog, notification.log()),
				// Contains instead of equals because the message is wrapped in HTML tags
				() -> assertTrue(notification.message().contains(expectedMessage))
				);
		
	}

	@Test
	void publishBookingRelatedEvent_throws_exception_when_notification_type_is_the_wrong_type_for_watch_alerts() {
		// Arrange
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, 
				() -> eventPublisher.publishBookingRelatedEvent(NotificationType.BOOKING_CANCELLED, WATCH_ALERT, EMAIL));
		
	}
}