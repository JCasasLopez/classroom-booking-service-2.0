package dev.jcasaslopez.booking.exception;

public class NoSuchClassroomException extends RuntimeException {
	public NoSuchClassroomException(String message) {
		super(message);
	}
}