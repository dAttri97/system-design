package com.attri.systemdesign.lld.venue_booking.policy;

import com.attri.systemdesign.lld.venue_booking.model.Booking;

import java.time.LocalDateTime;

/**
 * Strategy deciding whether a booking may still be cancelled.
 * <p>
 * Each venue owns one. New rules (fee-based, tier-based, weather exceptions) are added by
 * implementing this interface, with no change to {@code BookingService}.
 */
@FunctionalInterface
public interface CancellationPolicy {

    CancellationDecision evaluate(Booking booking, LocalDateTime now);

    /** Human-readable summary, e.g. for display on the venue page. */
    default String description() {
        return getClass().getSimpleName();
    }
}
