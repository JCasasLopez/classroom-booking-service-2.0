package dev.jcasaslopez.booking.service;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;

public interface NotificationOutboxService {
	
		void persistNotificationOutbox(NotificationType type, Booking booking, String email);
	    void persistNotificationOutbox(NotificationType type, WatchAlert watchAlert, String email);

}
