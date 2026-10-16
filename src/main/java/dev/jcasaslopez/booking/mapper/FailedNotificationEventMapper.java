package dev.jcasaslopez.booking.mapper;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.booking.entity.FailedNotificationEvent;
import dev.jcasaslopez.classroom.shared.event.NotificationEvent;

@Component
public class FailedNotificationEventMapper {

	public FailedNotificationEvent toEntity (NotificationEvent notificationEvent) {
		return (new FailedNotificationEvent 
				(0L,
						notificationEvent.subject(),
						notificationEvent.message(),
						notificationEvent.emailAddress()
						));
	}

	public NotificationEvent toKafkaRecord (FailedNotificationEvent failedNotificationEvent) { 
		return (new NotificationEvent 
				(failedNotificationEvent.getSubject(),
						failedNotificationEvent.getMessage(),
						failedNotificationEvent.getEmailAddress()
						));
	}

}
