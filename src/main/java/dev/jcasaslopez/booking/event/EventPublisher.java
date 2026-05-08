package dev.jcasaslopez.booking.event;

import dev.jcasaslopez.booking.domain.Booking;
import dev.jcasaslopez.booking.domain.WatchAlert;

public interface EventPublisher {
	
	void bookEventPublisher(Booking booking, String email);
	void cancelBookingEventPublisher(Booking booking, String email);
	void watchAlertEventPublisher(WatchAlert watchAlert, String email);

}
