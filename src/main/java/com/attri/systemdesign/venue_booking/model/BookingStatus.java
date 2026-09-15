package com.attri.systemdesign.venue_booking.model;

/** Lifecycle of a booking. Only {@link #CONFIRMED} bookings hold a slot. */
public enum BookingStatus {
    CONFIRMED,
    CANCELLED
}
