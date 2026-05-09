package dev.jcasaslopez.booking.event;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;

public interface EventPublisher {
	
	void bookEventPublisher(Booking booking, String email);
	void cancelBookingEventPublisher(Booking booking, String email);
	
	// Fired when a user creates a new watch alert
	void watchAlertEventPublisher(WatchAlert watchAlert, String email);
	
	// Fired when a booking is cancelled and a watch alert is activated
	void watchAlertTriggeredEventPublisher(Booking booking, String email);
}
