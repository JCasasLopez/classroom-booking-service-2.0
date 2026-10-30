package dev.jcasaslopez.booking.exception;

public class TimePointOutOfOpeningHoursException extends RuntimeException {
	public TimePointOutOfOpeningHoursException(String message) {
		super(message);
	}
}