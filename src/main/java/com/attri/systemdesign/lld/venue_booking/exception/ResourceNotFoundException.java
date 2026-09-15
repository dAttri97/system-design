package com.attri.systemdesign.lld.venue_booking.exception;

/** Raised when a referenced user, venue, court or booking does not exist. */
public class ResourceNotFoundException extends BookingSystemException {

    public ResourceNotFoundException(String type, String id) {
        super(type + " not found: " + id);
    }
}
