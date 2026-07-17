package dev.jcasaslopez.booking.kafka.event;

import dev.jcasaslopez.booking.entity.Booking;
import dev.jcasaslopez.booking.entity.WatchAlert;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;

public interface EventPublisher {
	
		void publishBookingRelatedEvent(NotificationType type, Booking booking, String email);
	    void publishBookingRelatedEvent(NotificationType type, WatchAlert watchAlert, String email);

}
