package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class InvalidBookingStatusException extends RuntimeException {
    public InvalidBookingStatusException(String message) {
        super(message);
    }
}