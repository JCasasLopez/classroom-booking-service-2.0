package dev.jcasaslopez.booking.kafka.event;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.booking.exception.NoSuchBookingException;
import dev.jcasaslopez.booking.exception.NoSuchClassroomException;
import dev.jcasaslopez.booking.kafka.producer.NotificationEventProducer;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

public class EventPublisherImpl implements EventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(EventPublisherImpl.class);
	private List<ClassroomEvent> classroomsStore;
	private NotificationEventProducer notificationEventProducer;
	private BookingRepository bookingRepository;

	public EventPublisherImpl(List<ClassroomEvent> classroomsStore, NotificationEventProducer notificationEventProducer,
			BookingRepository bookingRepository) {
		this.classroomsStore = classroomsStore;
		this.notificationEventProducer = notificationEventProducer;
		this.bookingRepository = bookingRepository;
	}

	@Override
	public void publishBookingRelatedEvent(NotificationType type, Booking booking, String email) {
		if (!Set.of(NotificationType.BOOKING_CONFIRMED, NotificationType.BOOKING_CANCELLED, 
				NotificationType.WATCH_ALERT_TRIGGERED).contains(type)) {	
			logger.warn("Invalid notification type {} for booking event", type);
			throw new IllegalArgumentException("Invalid notification type for booking event: " + type);
		}
		NotificationEvent notification = createNotificationEvent(type, booking, email);
		notificationEventProducer.sendNotification(notification);
	}

	@Override
	public void publishBookingRelatedEvent(NotificationType type, WatchAlert watchAlert, String email) {
		if (!Set.of(NotificationType.WATCH_ALERT_CONFIRMED).contains(type)) {
			logger.warn("Invalid notification type {} for booking event", type);
			throw new IllegalArgumentException("Invalid notification type for booking event: " + type);
		}
		Booking booking = bookingRepository.findById(watchAlert.getIdBooking())
				.orElseThrow(() -> new NoSuchBookingException("Booking was not found in the database"));		
		NotificationEvent notification = createNotificationEvent(type, booking, email);
		notificationEventProducer.sendNotification(notification);	
	}
	
	
	// *******************************************************************************************************
	// ****************************************** Auxiliary methods ******************************************
	// *******************************************************************************************************

	private NotificationEvent createNotificationEvent(NotificationType type, Booking booking, String email) {
		String subject = type.getSubject();

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");
		String dateStr = booking.getStart().format(dateFormatter);       
		String startTimeStr = booking.getStart().format(timeFormatter);  
		String endTimeStr = booking.getFinish().format(timeFormatter);    
		String message = String.format(type.getMessageText(), classroomName(booking.getIdClassroom()), dateStr, startTimeStr, endTimeStr);

		String log = String.format(type.getLogText(), email);

		logger.debug("Notification event created: {}", message);
		return new NotificationEvent(subject, message, email, log);
	}

	private String classroomName(int idClassroom) {
		logger.debug("Searching classroom name...");
		return classroomsStore.stream()
				.filter(classroom -> classroom.getIdClassroom() == idClassroom)
				.findAny()
				.orElseThrow(() -> new NoSuchClassroomException("Classroom was not found in the database")).getName();	
	}

}
