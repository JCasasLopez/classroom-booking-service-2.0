package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class ClassroomNotFoundException extends RuntimeException {
	public ClassroomNotFoundException(String message) {
		super(message);
	}
}