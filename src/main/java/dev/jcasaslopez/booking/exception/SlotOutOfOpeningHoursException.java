package dev.jcasaslopez.booking.exception;

public class SlotOutOfOpeningHoursException extends RuntimeException {
	public SlotOutOfOpeningHoursException(String message) {
		super(message);
	}
}

