package com.attri.systemdesign.lld.venue_booking.model;

/** Lifecycle of a booking. Only {@link #CONFIRMED} bookings hold a slot. */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
