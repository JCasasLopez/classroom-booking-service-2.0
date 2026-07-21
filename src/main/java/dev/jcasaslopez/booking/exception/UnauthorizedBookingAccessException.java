package dev.jcasaslopez.booking.exception;

public class UnauthorizedBookingAccessException extends RuntimeException {
    public UnauthorizedBookingAccessException(String message) {
        super(message);
    }
}