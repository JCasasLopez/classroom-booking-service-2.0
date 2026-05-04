package dev.jcasaslopez.booking.event;

public interface EventPublisher {
	
	void bookEventPublisher(String email);
	void cancelBookingEventPublisher(String email);
	void watchAlertEventPublisher(String email);

}
