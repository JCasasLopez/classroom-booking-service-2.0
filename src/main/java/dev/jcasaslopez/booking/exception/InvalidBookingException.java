package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class InvalidBookingException  extends RuntimeException {
	public InvalidBookingException(String message) {
		super(message);
	}
}