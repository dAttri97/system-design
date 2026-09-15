package com.attri.systemdesign.venue_booking.exception;

/** Raised when a slot is already reserved, i.e. the double-booking guard rejected the request. */
public class SlotUnavailableException extends BookingSystemException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
