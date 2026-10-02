package dev.jcasaslopez.booking.exception;

public class ClassroomNotFoundException extends RuntimeException {
	public ClassroomNotFoundException(String message) {
		super(message);
	}
}