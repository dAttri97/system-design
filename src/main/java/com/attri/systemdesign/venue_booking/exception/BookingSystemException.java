package com.attri.systemdesign.venue_booking.exception;

/**
 * Base type for every error the booking system raises deliberately.
 * Callers that only care about "the request was rejected" can catch this one type.
 */
public class BookingSystemException extends RuntimeException {

    public BookingSystemException(String message) {
        super(message);
    }
}
