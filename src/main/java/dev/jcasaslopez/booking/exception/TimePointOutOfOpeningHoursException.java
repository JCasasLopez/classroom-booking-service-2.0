package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class TimePointOutOfOpeningHoursException extends RuntimeException {
	public TimePointOutOfOpeningHoursException(String message) {
		super(message);
	}
}