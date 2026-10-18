package dev.jcasaslopez.booking.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.booking.exception.BookingNotFoundException;
import dev.jcasaslopez.booking.exception.ClassroomNotFoundException;
import dev.jcasaslopez.booking.mapper.NotificationOutboxMapper;
import dev.jcasaslopez.booking.repository.BookingRepository;
import dev.jcasaslopez.booking.repository.OutboxRepository;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;
import dev.jcasaslopez.classroom.shared.event.ClassroomEvent;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Service
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

	private static final Logger logger = LoggerFactory.getLogger(NotificationOutboxServiceImpl.class);
	
	private final List<ClassroomEvent> classroomsStore;
	private final BookingRepository bookingRepository;
	private final OutboxRepository outboxRepository;
	private final NotificationOutboxMapper mapper;

	public NotificationOutboxServiceImpl(
			List<ClassroomEvent> classroomsStore, 
			BookingRepository bookingRepository,
			OutboxRepository outboxRepository, 
			NotificationOutboxMapper mapper) {
		this.classroomsStore = classroomsStore;
		this.bookingRepository = bookingRepository;
		this.outboxRepository = outboxRepository;
		this.mapper = mapper;
	}

	@Override
	public void persistNotificationOutbox(NotificationType type, Booking booking, String email) {
		if (!Set.of(NotificationType.BOOKING_CONFIRMED, NotificationType.BOOKING_CANCELLED, 
				NotificationType.WATCH_ALERT_TRIGGERED).contains(type)) {	
			throw new IllegalArgumentException("Invalid notification type for booking event: " + type);
		}
		
		NotificationEvent notification = createNotificationEvent(type, booking, email);
		
		outboxRepository.save(mapper.toEntity(notification));
	}

	@Override
	public void persistNotificationOutbox(NotificationType type, WatchAlert watchAlert, String email) {
		if (!Set.of(NotificationType.WATCH_ALERT_CONFIRMED).contains(type)) {
			throw new IllegalArgumentException("Invalid notification type for booking event: " + type);
		}
		
		Booking booking = bookingRepository.findById(watchAlert.getIdBooking())
				.orElseThrow(() -> new BookingNotFoundException("Booking was not found in the database"));	
		
		NotificationEvent notification = createNotificationEvent(type, booking, email);
		
		outboxRepository.save(mapper.toEntity(notification));
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

		logger.debug("Notification event created: {}", message);
		return new NotificationEvent(subject, message, email);
	}

	private String classroomName(int idClassroom) {
		logger.debug("Searching classroom name...");
		return classroomsStore.stream()
				.filter(classroom -> classroom.idClassroom() == idClassroom)
				.findAny()
				.orElseThrow(() -> new ClassroomNotFoundException("Classroom was not found in the database")).name();	
	}

}
