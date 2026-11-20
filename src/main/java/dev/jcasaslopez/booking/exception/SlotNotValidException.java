package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class SlotNotValidException extends RuntimeException {
	public SlotNotValidException(String message) {
		super(message);
	}
}
