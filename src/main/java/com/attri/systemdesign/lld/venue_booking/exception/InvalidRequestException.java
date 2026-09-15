package com.attri.systemdesign.lld.venue_booking.exception;

/** Raised when the caller supplies malformed or semantically invalid input. */
public class InvalidRequestException extends BookingSystemException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
