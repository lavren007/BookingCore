package ru.bookingcore.booking_service.exception;

public class RetryExhaustedException extends RuntimeException {
    public RetryExhaustedException(String message) {
        super(message);
    }
    
    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
