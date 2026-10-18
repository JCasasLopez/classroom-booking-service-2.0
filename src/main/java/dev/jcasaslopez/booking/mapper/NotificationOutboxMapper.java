package dev.jcasaslopez.booking.mapper;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.entity.NotificationOutbox;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Component
public class NotificationOutboxMapper {

	public NotificationOutbox toEntity (NotificationEvent notificationEvent) {
		return (new NotificationOutbox 
				(0L,
						notificationEvent.subject(),
						notificationEvent.message(),
						notificationEvent.emailAddress()
						));
	}

	public NotificationEvent toKafkaRecord (NotificationOutbox failedNotificationEvent) { 
		return (new NotificationEvent 
				(failedNotificationEvent.getSubject(),
						failedNotificationEvent.getMessage(),
						failedNotificationEvent.getEmailAddress()
						));
	}

}
