package dev.jcasaslopez.booking.service;

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

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.NotificationOutbox;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.enums.BookingStatus;
import dev.jcasaslopez.booking.kafka.producer.NotificationEventProducer;
import dev.jcasaslopez.booking.mapper.NotificationOutboxMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.OutboxRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;

@ExtendWith(MockitoExtension.class)
public class NotificationOutboxServiceTest {
	
	@Mock NotificationEventProducer notificationEventProducer;
	@Mock BookingRepository bookingRepository;
	@Mock OutboxRepository outboxRepository;

	private NotificationOutboxServiceImpl service;
	private NotificationOutboxMapper mapper = new NotificationOutboxMapper();;
	
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
	    service = new NotificationOutboxServiceImpl(
	        allClassrooms,
	        bookingRepository,
	        outboxRepository, 
	        mapper
	    );
	}
	
	@ParameterizedTest
	@MethodSource("NotificationFieldsProvider")
	void event_publisher_builds_correct_message_for_booking_options(NotificationType type, String expectedMessage) {
		// Arrange
		
		// Act
		service.persistNotificationOutbox(type, BOOKING, EMAIL);
		
		// Assert		
		ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
		
		verify(outboxRepository).save(captor.capture());
		
		NotificationOutbox notification = captor.getValue();
		assertAll(
				() -> assertEquals(type.getSubject(), notification.getSubject()),
				// Contains instead of equals because the message is wrapped in HTML tags
				() -> assertTrue(notification.getMessage().contains(expectedMessage)),
				() -> assertEquals(EMAIL, notification.getEmailAddress())
				);
		
	}
	
	// Each argument: (expected subject, expected log, expected message fragment, notification type)
	static Stream<Arguments> NotificationFieldsProvider() {
		return Stream.of(
				Arguments.of(NotificationType.BOOKING_CONFIRMED,
						"We are pleased to confirm your booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00"),
				Arguments.of(NotificationType.BOOKING_CANCELLED,
						"We are pleased to confirm that your booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00 has been succesfully cancelled"),
				Arguments.of(NotificationType.WATCH_ALERT_TRIGGERED,
						"A booking for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00 has been cancelled. Hurry up and book it before someone else does!")
				);
	}

	@Test
	void event_publisher_throws_exception_when_notification_is_the_wrong_type() {
		// Arrange
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, 
				() -> service.persistNotificationOutbox(NotificationType.WATCH_ALERT_CONFIRMED, BOOKING, EMAIL));
		
	}
	
	// Tests the second overloaded method
	@Test
	void event_publisher_builds_correct_message_for_watch_alert_confirmed() {
		// Arrange
		when(bookingRepository.findById(WATCH_ALERT.getIdBooking())).thenReturn(Optional.of(BOOKING));
		String expectedSubject = NotificationType.WATCH_ALERT_CONFIRMED.getSubject();
		String expectedMessage = "We are pleased to confirm your watch alert for classroom Main Auditorium on the 11/5/2026 from 11:00 to 12:00. If the booking is cancelled you will be notified.";
	
		// Act
		service.persistNotificationOutbox(NotificationType.WATCH_ALERT_CONFIRMED, WATCH_ALERT, EMAIL);
		
		// Assert
		ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
		verify(outboxRepository).save(captor.capture());
		
		NotificationOutbox notification = captor.getValue();
		assertAll(
				() -> assertEquals(expectedSubject, notification.getSubject()),
				// Contains instead of equals because the message is wrapped in HTML tags
				() -> assertTrue(notification.getMessage().contains(expectedMessage))
				);
		
	}

	@Test
	void event_publisher_throws_exception_when_notification_type_is_wrong_for_watch_alerts() {
		// Arrange
		
		// Act & Assert
		assertThrows(IllegalArgumentException.class, 
				() -> service.persistNotificationOutbox(NotificationType.BOOKING_CANCELLED, WATCH_ALERT, EMAIL));
		
	}
}