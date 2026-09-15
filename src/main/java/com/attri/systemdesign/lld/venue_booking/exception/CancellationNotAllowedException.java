package com.attri.systemdesign.lld.venue_booking.exception;

/** Raised when a booking exists but the venue's cancellation policy refuses the request. */
public class CancellationNotAllowedException extends BookingSystemException {

    public CancellationNotAllowedException(String message) {
        super(message);
    }
}
