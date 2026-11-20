package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class TimePointMisalignedException extends RuntimeException {
	public TimePointMisalignedException(String message) {
		super(message);
	}
}