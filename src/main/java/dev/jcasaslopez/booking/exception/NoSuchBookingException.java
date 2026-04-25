package dev.jcasaslopez.booking.exception;

public class NoSuchBookingException  extends RuntimeException {
	public NoSuchBookingException(String message) {
		super(message);
	}
}
