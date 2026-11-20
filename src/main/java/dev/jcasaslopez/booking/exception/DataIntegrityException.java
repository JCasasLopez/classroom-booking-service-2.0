package dev.jcasaslopez.booking.exception;

@SuppressWarnings("serial")
public class DataIntegrityException extends RuntimeException {
    public DataIntegrityException(String message) {
        super(message);
    }
}