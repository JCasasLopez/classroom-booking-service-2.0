package dev.jcasaslopez.booking.kafka.event;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;
import dev.jcasaslopez.classroom.shared.enums.NotificationType;

public interface EventPublisher {
	
		void publishBookingRelatedEvent(NotificationType type, Booking booking, String email);
	    void publishBookingRelatedEvent(NotificationType type, WatchAlert watchAlert, String email);

}
